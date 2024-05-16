package com.myorg;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class AwsRekognitionJavaLambda implements RequestHandler<S3Event, String> {

    private static final float MIN_CONFIDENCE = 50.0f;
    private static final int MAX_LABELS = 10;
    private static final AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();
    private static final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
    private static final AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClientBuilder.defaultClient();
    private static final DynamoDB dynamoDB = new DynamoDB(dynamoDBClient);

    @Override
    public String handleRequest(S3Event event, Context context) {
        context.getLogger().log("Lambda processing event: " + event);

        // For each message (photo) get the bucket name and key
        for (S3Event.S3EventNotificationRecord record : event.getRecords()) {
            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getKey().replace("%3A", ":");

            // For each bucket/key, retrieve labels
            rekFunction(bucket, key);
        }

        return "Successfully processed records.";
    }

    private static void rekFunction(String bucket, String key) {
        System.out.println("Currently processing the following image");
        System.out.println("Bucket: " + bucket + " key name: " + key);

        // Try and retrieve labels from Amazon Rekognition, using the confidence level we set in minConfidence var
        try {
            DetectLabelsRequest request = new DetectLabelsRequest()
                    .withImage(new Image().withS3Object(new com.amazonaws.services.rekognition.model.S3Object().withBucket(bucket).withName(key)))
                    .withMaxLabels(MAX_LABELS)
                    .withMinConfidence(MIN_CONFIDENCE);

            DetectLabelsResult result = rekognitionClient.detectLabels(request);
            List<Label> labels = result.getLabels();

            // Create our map for our label construction
            Map<String, String> imageLabels = new HashMap<>();

            // Add all of our labels into imageLabels
            int objectNum = 1;
            for (Label label : labels) {
                String itemAtt = "object" + objectNum++;
                imageLabels.put(itemAtt, label.getName());
            }

            // Instantiate a table resource object of our environment variable
            String tableName = System.getenv("TABLE");
            Table table = dynamoDB.getTable(tableName);

            // Put item into table
            Item item = new Item()
                    .withPrimaryKey("image", key)
                    .withMap("Item", imageLabels);
            table.putItem(item);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
