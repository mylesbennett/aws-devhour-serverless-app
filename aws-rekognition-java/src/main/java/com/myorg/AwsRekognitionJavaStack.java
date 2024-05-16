package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.PolicyStatementProps;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.S3EventSourceProps;
import software.amazon.awscdk.services.lambda.eventsources.S3EventSourceV2;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;
import software.amazon.awscdk.services.s3.EventType;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class AwsRekognitionJavaStack extends Stack {

    public AwsRekognitionJavaStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public AwsRekognitionJavaStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        String imageBucketName = "image-bucket-aimicor-java";
        String imageTableName = "image-table-aimicor-java";
        String imageLambdaName = "image-lambda-aimicor-java";

        // =====================================================================================
        // Image Bucket
        // =====================================================================================
        Bucket imageBucket = new Bucket(this, imageBucketName, BucketProps.builder()
                .removalPolicy(RemovalPolicy.DESTROY)
                .versioned(true)
                .build()
        );
        new CfnOutput(this, "imageBucket", CfnOutputProps.builder()
                .value(imageBucket.getBucketName())
                .build()
        );

        // =====================================================================================
        // Amazon DynamoDB table for storing image labels
        // =====================================================================================
        Table table = new Table(this, imageTableName, TableProps.builder()
                .partitionKey(Attribute.builder()
                        .name("image")
                        .type(AttributeType.STRING)
                        .build()
                )
                .removalPolicy(RemovalPolicy.DESTROY)
                .build()
        );
        new CfnOutput(this, "ddbTable", CfnOutputProps.builder()
                .value(table.getTableName())
                .build()
        );

        // =====================================================================================
        // Building our AWS Lambda Function; compute for our serverless microservice
        // =====================================================================================
        String lambdaPackage = "com.myorg";

        // Create a Function object
        Function rekFn = Function.Builder.create(this, imageLambdaName)
                .architecture(Architecture.ARM_64)
                .code(Code.fromAsset("lambda/target/lambda-0.1.jar"))
                .handler(lambdaPackage + ".AwsRekognitionJavaLambda::handleRequest")
                .runtime(Runtime.JAVA_11) // Specify the Java runtime
                .memorySize(1024) // Adjust memory size as needed
                .timeout(Duration.seconds(30)) // Adjust timeout as needed
                .environment(Map.of(
                        "TABLE", table.getTableName(),
                        "BUCKET", imageBucket.getBucketName()
                ))
                .build();


        rekFn.addEventSource(new S3EventSourceV2(imageBucket, S3EventSourceProps.builder()
                .events(List.of(EventType.OBJECT_CREATED))
                .build()
        ));
        imageBucket.grantRead(rekFn);
        table.grantWriteData(rekFn);

        rekFn.addToRolePolicy(new PolicyStatement(PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .actions(List.of("rekognition:DetectLabels"))
                .resources(List.of("*"))
                .build()
        ));
    }
}
