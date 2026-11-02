package com.jopagima.school.infra.students;


import software.amazon.awscdk.services.cloudwatch.Metric;
import software.amazon.awscdk.services.cloudwatch.MetricOptions;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.Function;
import software.constructs.Construct;
import software.amazon.awscdk.services.cloudwatch.Alarm;
import software.amazon.awscdk.services.cloudwatch.CreateAlarmOptions;

/**
 * StudentsAlertsConstruct
 */
public class StudentsAlertsConstruct extends Construct {

    public StudentsAlertsConstruct(Construct scope, String id, Function registerStudent, Function photoUpload) {
        super(scope, id);
        createAlarm("RegisterStudentErrorsAlarm", registerStudent);
        createAlarm( "PhotoUploadErrorsAlarm", photoUpload);
    }

    private Alarm createAlarm(String alarmId, Function function) {
        Metric functionMetric = errorsMetricOf(function);    

        return functionMetric.createAlarm(this, alarmId, 
            CreateAlarmOptions.builder()
                .threshold(1)
                .evaluationPeriods(1)
                .build());
    }

    private Metric errorsMetricOf(Function function) {
        return function.metricErrors(MetricOptions.builder()
                .period(Duration.minutes(5))
                .statistic("Sum")
                .build());
    }

}
