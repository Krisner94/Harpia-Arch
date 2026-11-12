package io.github.krisner94.harpia.deployment;

import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.github.krisner94.harpia.annotation.Activity;
import io.github.krisner94.harpia.annotation.Component;
import io.github.krisner94.harpia.annotation.Controller;
import io.github.krisner94.harpia.annotation.Repository;
import io.github.krisner94.harpia.annotation.Service;
import io.github.krisner94.harpia.annotation.WorkflowDefinition;
import java.util.List;
import org.jboss.jandex.DotName;

/**
 * Quarkus build steps of the Harpia Arch extension.
 */
class HarpiaArchProcessor {

    private static final String FEATURE = "harpia-arch";

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> producer) {
        producer.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple(Controller.class.getName())));
        producer.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple(Service.class.getName())));
        producer.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple(Repository.class.getName())));
        producer.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple(Component.class.getName())));
        producer.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple(Activity.class.getName())));
        producer.produce(new BeanDefiningAnnotationBuildItem(
                DotName.createSimple(WorkflowDefinition.class.getName())));
    }

    @BuildStep
    FeatureBuildItem validateArchitecture(CombinedIndexBuildItem combinedIndex) {
        List<String> errors = new ArchitectureValidator().validate(combinedIndex.getIndex());
        if (!errors.isEmpty()) {
            throw new IllegalStateException(String.join(System.lineSeparator(), errors));
        }
        return new FeatureBuildItem(FEATURE);
    }
}
