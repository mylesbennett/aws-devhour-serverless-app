package main

import (
	"github.com/aws/aws-cdk-go/awscdk/v2"
	"github.com/aws/aws-cdk-go/awscdk/v2/awsdynamodb"
	"github.com/aws/aws-cdk-go/awscdk/v2/awsiam"
	"github.com/aws/aws-cdk-go/awscdk/v2/awslambda"
	"github.com/aws/aws-cdk-go/awscdk/v2/awslambdaeventsources"
	"github.com/aws/aws-cdk-go/awscdk/v2/awss3"
	"github.com/aws/constructs-go/constructs/v10"
	"github.com/aws/jsii-runtime-go"
)

type AwsRekognitionGoStackProps struct {
	awscdk.StackProps
}

func NewAwsRekognitionGoStack(scope constructs.Construct, id string, props *AwsRekognitionGoStackProps) awscdk.Stack {
	var sprops awscdk.StackProps
	if props != nil {
		sprops = props.StackProps
	}
	stack := awscdk.NewStack(scope, &id, &sprops)

	// =====================================================================================
	// Image Bucket
	// =====================================================================================
	bucketProps := &awss3.BucketProps{
		RemovalPolicy: awscdk.RemovalPolicy_DESTROY,
		Versioned:     jsii.Bool(true),
	}
	imageBucket := awss3.NewBucket(stack, imageBucketName, bucketProps)

	// =====================================================================================
	// Amazon DynamoDB table for storing image labels
	// =====================================================================================
	tableProps := &awsdynamodb.TableProps{
		PartitionKey: &awsdynamodb.Attribute{
			Name: jsii.String("image"),
			Type: awsdynamodb.AttributeType_STRING,
		},
		RemovalPolicy: awscdk.RemovalPolicy_DESTROY,
	}
	table := awsdynamodb.NewTable(stack, imageTableName, tableProps)

	// =====================================================================================
	// Building our AWS Lambda Function; compute for our serverless microservice
	// =====================================================================================
	rekFn := awslambda.NewFunction(stack, jsii.String("rekognitionFunction"), &awslambda.FunctionProps{
		Code:       awslambda.Code_FromAsset(jsii.String("rekognitionlambda"), nil),
		Runtime:    awslambda.Runtime_PYTHON_3_12(),
		Handler:    jsii.String("index.handler"),
		Timeout:    awscdk.Duration_Seconds(jsii.Number(30)),
		MemorySize: jsii.Number(1024),
		Environment: &map[string]*string{
			"TABLE":  table.TableName(),
			"BUCKET": imageBucket.BucketName(),
		},
	})
	// Define event source properties
	eventSourceProps := &awslambdaeventsources.S3EventSourceProps{
		Events: &[]awss3.EventType{awss3.EventType_OBJECT_CREATED},
	}
	// Create the S3 event source
	rekFn.AddEventSource(awslambdaeventsources.NewS3EventSource(imageBucket, eventSourceProps))
	imageBucket.GrantRead(rekFn, nil)
	table.GrantWriteData(rekFn)

	rolePolicy := &awsiam.PolicyStatementProps{
		Effect:    awsiam.Effect_ALLOW,
		Actions:   &[]*string{jsii.String("rekognition:DetectLabels")},
		Resources: &[]*string{jsii.String("*")},
	}
	rekFn.AddToRolePolicy(awsiam.NewPolicyStatement(rolePolicy))

	return stack
}

func main() {
	defer jsii.Close()

	app := awscdk.NewApp(nil)

	NewAwsRekognitionGoStack(app, "AwsRekognitionGoStack", &AwsRekognitionGoStackProps{
		awscdk.StackProps{
			Env: env(),
			Synthesizer: awscdk.NewDefaultStackSynthesizer(&awscdk.DefaultStackSynthesizerProps{
				GenerateBootstrapVersionRule: jsii.Bool(false),
			}),
		},
	})

	app.Synth(nil)
}

// env determines the AWS environment (account+region) in which our stack is to
// be deployed. For more information see: https://docs.aws.amazon.com/cdk/latest/guide/environments.html
func env() *awscdk.Environment {
	// If unspecified, this stack will be "environment-agnostic".
	// Account/Region-dependent features and context lookups will not work, but a
	// single synthesized template can be deployed anywhere.
	//---------------------------------------------------------------------------
	return nil

	// Uncomment if you know exactly what account and region you want to deploy
	// the stack to. This is the recommendation for production stacks.
	//---------------------------------------------------------------------------
	// return &awscdk.Environment{
	//  Account: jsii.String("231945619229"),
	//  Region:  jsii.String("eu-west-1"),
	// }

	// Uncomment to specialize this stack for the AWS Account and Region that are
	// implied by the current CLI configuration. This is recommended for dev
	// stacks.
	//---------------------------------------------------------------------------
	// return &awscdk.Environment{
	//  Account: jsii.String(os.Getenv("CDK_DEFAULT_ACCOUNT")),
	//  Region:  jsii.String(os.Getenv("CDK_DEFAULT_REGION")),
	// }
}
