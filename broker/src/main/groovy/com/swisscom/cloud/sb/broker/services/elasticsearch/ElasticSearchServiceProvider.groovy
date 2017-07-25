package com.swisscom.cloud.sb.broker.services.elasticsearch

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Optional
import com.swisscom.cloud.sb.broker.binding.BindRequest
import com.swisscom.cloud.sb.broker.binding.BindResponse
import com.swisscom.cloud.sb.broker.binding.UnbindRequest
import com.swisscom.cloud.sb.broker.model.ProvisionRequest
import com.swisscom.cloud.sb.broker.model.ServiceDetail
import com.swisscom.cloud.sb.broker.model.ServiceInstance
import com.swisscom.cloud.sb.broker.provisioning.async.AsyncOperationResult
import com.swisscom.cloud.sb.broker.provisioning.lastoperation.LastOperationJobContext
import com.swisscom.cloud.sb.broker.provisioning.statemachine.ServiceStateWithAction
import com.swisscom.cloud.sb.broker.provisioning.statemachine.StateMachine
import com.swisscom.cloud.sb.broker.services.bosh.BoshBasedServiceProvider
import com.swisscom.cloud.sb.broker.services.bosh.BoshTemplate
import com.swisscom.cloud.sb.broker.services.bosh.statemachine.BoshStateMachineFactory
import com.swisscom.cloud.sb.broker.services.elasticsearch.statemachine.ElasticSearchDeprovisionState
import com.swisscom.cloud.sb.broker.services.elasticsearch.statemachine.ElasticSearchProvisionState
import com.swisscom.cloud.sb.broker.services.elasticsearch.statemachine.ElasticSearchStateMachineContext
import com.swisscom.cloud.sb.broker.util.ServiceDetailKey
import com.swisscom.cloud.sb.broker.util.ServiceDetailsHelper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

@Component
@CompileStatic
@Slf4j
class ElasticSearchServiceProvider extends BoshBasedServiceProvider<ElasticSearchConfig> {
    public static final String SEARCHGUARD_USERNAME = 'searchguard-username'
    public static final String SEARCHGUARD_PASSWORD = 'searchguard-password'
    public static final String PORT_HTTP = 'http-port'
    public static final String PORT_INTERNAL = 'internal-port'
    public static final String PORT_MGMT = 'mgmt-port'


    @Autowired
    protected ElasticSearchFreePortFinder elasticSearchFreePortFinder

    @PostConstruct
    void init() {
        log.info(serviceConfig.toString())
    }

    @Override
    Collection<ServiceDetail> customizeBoshTemplate(BoshTemplate template, ProvisionRequest provisionRequest) {
        ServiceInstance serviceInstance = provisioningPersistenceService.getServiceInstance(provisionRequest.serviceInstanceGuid)

        template.replace(SEARCHGUARD_USERNAME, ServiceDetailsHelper.from(serviceInstance.details).getValue(ServiceDetailKey.ELASTIC_SEARCH_USER))
        template.replace(SEARCHGUARD_PASSWORD, ServiceDetailsHelper.from(serviceInstance.details).getValue(ServiceDetailKey.ELASTIC_SEARCH_PASSWORD))
        template.replace(PORT_HTTP, ServiceDetailsHelper.from(serviceInstance.details).getValue(ServiceDetailKey.ELASTIC_SEARCH_PORT))
        template.replace(PORT_INTERNAL, ServiceDetailsHelper.from(serviceInstance.details).getValue(ServiceDetailKey.ELASTIC_SEARCH_PORT_INTERNAL))
        template.replace(PORT_MGMT, ServiceDetailsHelper.from(serviceInstance.details).getValue(ServiceDetailKey.ELASTIC_SEARCH_PORT_MGMT))

        return [ServiceDetail.from(ServiceDetailKey.ELASTIC_SEARCH_HOST, template.instanceCount() as String)]
    }

    @Override
    AsyncOperationResult requestProvision(LastOperationJobContext context) {
        StateMachine stateMachine = createProvisionStateMachine(context)
        ServiceStateWithAction currentState = getProvisionState(context)
        def actionResult = stateMachine.setCurrentState(currentState, createStateMachineContext(context))
        return AsyncOperationResult.of(actionResult.go2NextState ? stateMachine.nextState(currentState) : currentState, actionResult.details)
    }

    @Override
    Optional<AsyncOperationResult> requestDeprovision(LastOperationJobContext context) {
        StateMachine stateMachine = createDeprovisionStateMachine(context)
        ServiceStateWithAction currentState = getDeprovisionState(context)
        def actionResult = stateMachine.setCurrentState(currentState, createStateMachineContext(context))
        return Optional.of(AsyncOperationResult.of(actionResult.go2NextState ? stateMachine.nextState(currentState) : currentState, actionResult.details))
    }

    @Override
    BindResponse bind(BindRequest request) {
        return null
    }

    @Override
    void unbind(UnbindRequest request) {

    }

    @VisibleForTesting
    private ElasticSearchStateMachineContext createStateMachineContext(LastOperationJobContext context) {
        return new ElasticSearchStateMachineContext(elasticSearchConfig: serviceConfig,
                elasticSearchFreePortFinder: elasticSearchFreePortFinder,
                boshFacade: getBoshFacade(),
                boshTemplateCustomizer: this,
                lastOperationJobContext: context)
    }

    @VisibleForTesting
    private StateMachine createProvisionStateMachine(LastOperationJobContext context) {
        StateMachine stateMachine = new StateMachine([ElasticSearchProvisionState.FIND_PORTS, ElasticSearchProvisionState.GENERATE_USERNAME_PASSWORD])
        stateMachine.addAllFromStateMachine(BoshStateMachineFactory.createProvisioningStateFlow(getBoshFacade().shouldCreateOpenStackServerGroup(context)))
        stateMachine.addAll([ElasticSearchProvisionState.PROVISION_SUCCESS])
        return stateMachine
    }

    @VisibleForTesting
    private ServiceStateWithAction getProvisionState(LastOperationJobContext context) {
        ServiceStateWithAction provisionState = null
        if (!context.lastOperation.internalState) {
            provisionState = ElasticSearchProvisionState.FIND_PORTS
        } else {
            provisionState = ElasticSearchProvisionState.of(context.lastOperation.internalState)
        }
        return provisionState
    }

    @VisibleForTesting
    private StateMachine createDeprovisionStateMachine(LastOperationJobContext context) {
        StateMachine stateMachine = BoshStateMachineFactory.createDeprovisioningStateFlow(getBoshFacade().shouldCreateOpenStackServerGroup(context))
        stateMachine.addAll([ElasticSearchDeprovisionState.DEPROVISION_SUCCESS])
    }

    @VisibleForTesting
    private ServiceStateWithAction getDeprovisionState(LastOperationJobContext context) {
        ServiceStateWithAction deprovisionState = null
        if (!context.lastOperation.internalState) {
            deprovisionState = createDeprovisionStateMachine(context).states.first()
        } else {
            deprovisionState = ElasticSearchDeprovisionState.of(context.lastOperation.internalState)
        }
        return deprovisionState
    }
}