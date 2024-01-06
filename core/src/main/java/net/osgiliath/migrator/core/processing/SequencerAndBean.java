package net.osgiliath.migrator.core.processing;

import net.osgiliath.migrator.core.configuration.AbstractTransformationConfigurationDefinition;

public class SequencerAndBean {
    private final AbstractTransformationConfigurationDefinition sequencerConfiguration;
    private final Object bean;

    public SequencerAndBean(AbstractTransformationConfigurationDefinition sequencerConfiguration, Object bean) {
        this.sequencerConfiguration = sequencerConfiguration;
        this.bean = bean;
    }

    public AbstractTransformationConfigurationDefinition getSequencerConfiguration() {
        return sequencerConfiguration;
    }

    public Object getBean() {
        return bean;
    }
}
