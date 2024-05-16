# Welcome to your CDK Java project!

Java version of of the tutorial at https://github.com/aws-samples/aws-dev-hour-backend.git and https://pages.awscloud.com/global-traincert-twitch-dev-hour-building-modern-applications.html

It turns out that Java lambdas require their own sub-project, packaged into an uber-jar and then that jar being referenced as the source for the lambda object in the stack code. To build and deploy this project from command-line at repo route:

- cd lambda
- mvn package
- cd ../
- cdk deploy

The `cdk.json` file tells the CDK Toolkit how to execute your app.

It is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.

## Useful commands

 * `mvn package`     compile and run tests
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk docs`        open CDK documentation

Enjoy!
