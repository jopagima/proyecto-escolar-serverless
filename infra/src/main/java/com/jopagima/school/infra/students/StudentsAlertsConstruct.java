package com.jopagima.school.infra.students;


import software.amazon.awscdk.services.cloudwatch.Metric;
import software.amazon.awscdk.services.cloudwatch.MetricOptions;
import software.amazon.awscdk.services.cloudwatch.TreatMissingData;
import software.amazon.awscdk.services.cloudwatch.actions.SnsAction;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.sns.Topic;
import software.constructs.Construct;
import software.amazon.awscdk.services.cloudwatch.Alarm;
import software.amazon.awscdk.services.cloudwatch.CreateAlarmOptions;

/**
 * StudentsAlertsConstruct
 */
public class StudentsAlertsConstruct extends Construct {

    public StudentsAlertsConstruct(Construct scope, String id, Function registerStudent, Function photoUpload) {
        super(scope, id);
        Topic operationalAlertsTopic = new Topic(this, "OperationalAlertsTopic");
        createAlarm("RegisterStudentErrorsAlarm", registerStudent, operationalAlertsTopic);
        createAlarm("PhotoUploadErrorsAlarm", photoUpload, operationalAlertsTopic);
    }

    private void createAlarm(String alarmId, Function function, Topic topic) {
        Metric functionMetric = errorsMetricOf(function);    

        Alarm alarm = functionMetric.createAlarm(this, alarmId, 
            CreateAlarmOptions.builder()
                .threshold(1)
                .evaluationPeriods(1)
                .treatMissingData(TreatMissingData.NOT_BREACHING)
                .build());
        alarm.addAlarmAction(new SnsAction(topic));
    }

    private Metric errorsMetricOf(Function function) {
        return function.metricErrors(MetricOptions.builder()
                .period(Duration.minutes(5))
                .statistic("Sum")
                .build());
    }

}
