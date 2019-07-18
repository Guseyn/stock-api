package com.unloadbrain.assignement.payconiq.akka;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;
import akka.actor.Props;
import org.springframework.context.ApplicationContext;

public class AkkaSpringExtension extends AbstractExtensionId<AkkaSpringExtension.SpringExt> {

    private final ApplicationContext applicationContext;

    public AkkaSpringExtension(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public SpringExt createExtension(ExtendedActorSystem system) {
        return new SpringExt();
    }

    public class SpringExt implements Extension {

        private volatile ApplicationContext applicationContext;

        public void initialize(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        /**
         * Provide Spring-managed actor reference.
         *
         * @param actorBeanName bean name.
         * @return Props is the blueprint for an Actor.
         */
        public Props props(String actorBeanName) {
            return Props.create(SpringActorProducer.class, applicationContext, actorBeanName);
        }

    }

}