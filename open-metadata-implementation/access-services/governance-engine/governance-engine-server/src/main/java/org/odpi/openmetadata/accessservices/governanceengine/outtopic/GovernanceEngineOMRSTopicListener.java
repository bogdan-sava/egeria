/* SPDX-License-Identifier: Apache 2.0 */
/* Copyright Contributors to the ODPi Egeria project. */

package org.odpi.openmetadata.accessservices.governanceengine.outtopic;

import org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper;
import org.odpi.openmetadata.frameworks.auditlog.AuditLog;
import org.odpi.openmetadata.frameworks.governanceaction.events.WatchdogClassificationEvent;
import org.odpi.openmetadata.frameworks.governanceaction.events.WatchdogEventType;
import org.odpi.openmetadata.frameworks.governanceaction.events.WatchdogMetadataElementEvent;
import org.odpi.openmetadata.frameworks.governanceaction.events.WatchdogRelatedElementsEvent;
import org.odpi.openmetadata.frameworks.governanceaction.properties.ElementClassification;
import org.odpi.openmetadata.frameworks.governanceaction.properties.OpenMetadataElement;
import org.odpi.openmetadata.frameworks.governanceaction.properties.RelatedMetadataElements;
import org.odpi.openmetadata.repositoryservices.connectors.omrstopic.OMRSTopicListenerBase;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.*;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDefSummary;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;

import java.util.List;

/**
 * GovernanceEngineOMRSTopicListener is the listener that registers with the repository services (OMRS)
 * to monitor changes to the metadata.  This class is looking for changes to the governance engines
 * and governance services configuration.  When it detects an appropriate change it passes the
 * update to the GovernanceEngineOutTopicPublisher to publish a GovernanceEngineEvent describing the
 * change.  These events are picked up by the Engine Host Server (See engine-host-services) to
 * control the configuration of the governance engines it is hosting.
 */
public class GovernanceEngineOMRSTopicListener extends OMRSTopicListenerBase
{
    private GovernanceEngineOutTopicPublisher eventPublisher;
    private OMRSRepositoryHelper              repositoryHelper;

    private EntityDetail nullEntity = null;
    private Relationship nullRelationship = null;

    /**
     * Initialize the topic listener.
     *
     * @param serviceName this is the full name of the service - used for error logging in base class
     * @param eventPublisher this is the out topic publisher.
     * @param repositoryHelper repository helper
     * @param auditLog logging destination
     */
    public GovernanceEngineOMRSTopicListener(String                            serviceName,
                                             GovernanceEngineOutTopicPublisher eventPublisher,
                                             OMRSRepositoryHelper              repositoryHelper,
                                             AuditLog                          auditLog)
    {
        super(serviceName, auditLog);

        this.eventPublisher   = eventPublisher;
        this.repositoryHelper = repositoryHelper;
    }


    /**
     * Process an entity extracted from an event.
     *
     * @param sourceName source of the event
     * @param entity entity from the event
     * @param methodName calling method (indicates type of event action)
     * @return boolean flag indicating that the event is processed
     */
    private boolean processGovernanceEngineEvent(String         sourceName,
                                                 EntityDetail   entity,
                                                 String         methodName)
    {
        if (entity != null)
        {
            InstanceType type = entity.getType();

            if (type != null)
            {
                if (repositoryHelper.isTypeOf(sourceName,
                                              type.getTypeDefName(),
                                              OpenMetadataAPIMapper.GOVERNANCE_ENGINE_TYPE_NAME))
                {
                    eventPublisher.publishRefreshGovernanceEngineEvent(entity.getGUID(),
                                                                      repositoryHelper.getStringProperty(sourceName,
                                                                                                         OpenMetadataAPIMapper.QUALIFIED_NAME_PROPERTY_NAME,
                                                                                                         entity.getProperties(),
                                                                                                         methodName));
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Process a relationship extracted from an event.
     *
     * @param sourceName source of the event
     * @param relationship relationship from the event
     * @param methodName calling method (indicates type of event action)
     * @return boolean flag indicating that the event is processed
     */
    private boolean processSupportedGovernanceService(String       sourceName,
                                                      Relationship relationship,
                                                      String       methodName)
    {
        if (relationship != null)
        {
            InstanceType type = relationship.getType();

            if (type != null)
            {
                if (repositoryHelper.isTypeOf(sourceName,
                                              type.getTypeDefName(),
                                              OpenMetadataAPIMapper.SUPPORTED_GOVERNANCE_SERVICE_TYPE_NAME))
                {
                    EntityProxy end2 = relationship.getEntityTwoProxy();

                    if (end2 != null)
                    {
                        eventPublisher.publishRefreshGovernanceServiceEvent(end2.getGUID(),
                                                                           repositoryHelper.getStringProperty(sourceName,
                                                                                                              OpenMetadataAPIMapper.QUALIFIED_NAME_PROPERTY_NAME,
                                                                                                              end2.getUniqueProperties(),
                                                                                                              methodName),
                                                                           relationship.getGUID(),
                                                                           repositoryHelper.getStringProperty(sourceName,
                                                                                                              OpenMetadataAPIMapper.REQUEST_TYPE_PROPERTY_NAME,
                                                                                                              relationship.getProperties(),
                                                                                                              methodName));
                        return true;
                    }
                }
            }
        }

        return false;
    }



    /**
     * Process an entity extracted from an event.
     *
     * @param sourceName source of the event
     * @param entity entity from the event
     * @param methodName calling method (indicates type of event action)
     * @return boolean flag indicating that the event should be ignored
     */
    private boolean processGovernanceActionEvent(String       sourceName,
                                                 EntityDetail entity,
                                                 String       methodName)
    {
        if (entity != null)
        {
            InstanceType type = entity.getType();

            if (type != null)
            {
                if (repositoryHelper.isTypeOf(sourceName,
                                              type.getTypeDefName(),
                                              OpenMetadataAPIMapper.GOVERNANCE_ACTION_TYPE_NAME))
                {
                    // todo call governance action handler
                    // todo if updated still may be new because restart called
                    return true;
                }
            }
        }

        return false;
    }



    /**
     * Ignore events from entities that are part of the governance action processing.
     * (Called after the GovernanceAction events are handled.)
     * May not need this.
     *
     * @param sourceName source of the event
     * @param entity entity from the event
     * @return boolean flag indicating that the event should be ignored
     */
    private boolean excludeGovernanceManagementEvents(String       sourceName,
                                                      EntityDetail entity)
    {
        if (entity != null)
        {
            InstanceType type = entity.getType();

            if (type != null)
            {
                return repositoryHelper.isTypeOf(sourceName,
                                                 type.getTypeDefName(),
                                                 OpenMetadataAPIMapper.GOVERNANCE_ACTION_TYPE_TYPE_NAME);
            }
        }

        return false;
    }


    /**
     * Ignore events from relationships that are part of the governance action processing.
     *
     * @param sourceName source of the event
     * @param relationship relationship from the event
     * @return boolean flag indicating that the event should be ignored
     */
    private boolean excludeGovernanceManagementEvents(String            sourceName,
                                                      Relationship      relationship)
    {
        if (relationship != null)
        {
            InstanceType type = relationship.getType();

            if (type != null)
            {
                if (repositoryHelper.isTypeOf(sourceName,
                                              type.getTypeDefName(),
                                              OpenMetadataAPIMapper.SUPPORTED_GOVERNANCE_SERVICE_TYPE_NAME))
                {
                    return true;
                }

                if (repositoryHelper.isTypeOf(sourceName,
                                              type.getTypeDefName(),
                                              OpenMetadataAPIMapper.GOVERNANCE_ACTION_TYPE_USE_TYPE_NAME))
                {
                    return true;
                }

                if (repositoryHelper.isTypeOf(sourceName,
                                              type.getTypeDefName(),
                                              OpenMetadataAPIMapper.TARGET_FOR_ACTION_TYPE_NAME))
                {
                    return true;
                }

                if (repositoryHelper.isTypeOf(sourceName,
                                              type.getTypeDefName(),
                                              OpenMetadataAPIMapper.GOVERNANCE_ACTION_REQUEST_SOURCE_TYPE_NAME))
                {
                    return true;
                }


                return (repositoryHelper.isTypeOf(sourceName,
                                                  type.getTypeDefName(),
                                                  OpenMetadataAPIMapper.GOVERNANCE_ACTION_EXECUTOR_TYPE_NAME));
            }
        }

        return false;
    }


    /**
     * Using the content of the entity, create a metadata element.
     *
     * @param sourceName source of the event
     * @param entity entity from the repository
     * @param methodName calling method
     * @return open metadata element object
     */
    private OpenMetadataElement getMetadataElement(String       sourceName,
                                                   EntityDetail entity,
                                                   String       methodName)
    {
        // todo - call converter
        return null;
    }


    /**
     * Using the content of the classification, create an element classification.
     *
     * @param sourceName source of the event
     * @param classification from the repository services
     * @param methodName calling method
     * @return open metadata element object
     */
    private ElementClassification getClassification(String         sourceName,
                                                    Classification classification,
                                                    String         methodName)
    {
        // todo - call converter
        return null;
    }


    /**
     * Using the content of the entity, create a metadata element.
     *
     * @param sourceName source of the event
     * @param relationship relationship from the repository
     * @param methodName calling method
     * @return open metadata element object
     */
    private RelatedMetadataElements getRelatedElements(String       sourceName,
                                                       Relationship relationship,
                                                       String       methodName)
    {
        // todo - call converter
        return null;
    }


    /**
     * Process an entity extracted from an event.
     *
     * @param sourceName source of the event
     * @param eventType watchdog event type
     * @param entity entity from the event
     * @param previousEntity set up for update events
     * @param methodName calling method (indicates type of event action)
     */
    private void processWatchdogEvent(String            sourceName,
                                      WatchdogEventType eventType,
                                      EntityDetail      entity,
                                      EntityDetail      previousEntity,
                                      String            methodName)
    {
        if (entity != null)
        {
            InstanceType type = entity.getType();

            if (type != null)
            {
                WatchdogMetadataElementEvent watchdogEvent = new WatchdogMetadataElementEvent();

                watchdogEvent.setEventType(eventType);
                watchdogEvent.setMetadataElement(this.getMetadataElement(sourceName, entity, methodName));

                if (previousEntity != null)
                {
                    watchdogEvent.setPreviousMetadataElement(this.getMetadataElement(sourceName, previousEntity, methodName));
                }
            }
        }
    }


    /**
     * Process a classification extracted from an event.
     *
     * @param sourceName source of the event
     * @param eventType watchdog event type
     * @param entity entity from the event
     * @param classification from the event
     * @param previousClassification set up for update events
     * @param methodName calling method (indicates type of event action)
     */
    private void processWatchdogEvent(String            sourceName,
                                      WatchdogEventType eventType,
                                      EntityDetail      entity,
                                      Classification    classification,
                                      Classification    previousClassification,
                                      String            methodName)
    {
        if (entity != null)
        {
            InstanceType type = entity.getType();

            if (type != null)
            {
                WatchdogClassificationEvent watchdogEvent = new WatchdogClassificationEvent();

                watchdogEvent.setEventType(eventType);
                watchdogEvent.setMetadataElement(this.getMetadataElement(sourceName, entity, methodName));
                watchdogEvent.setChangedClassification(this.getClassification(sourceName, classification, methodName));

                if (previousClassification != null)
                {
                    watchdogEvent.setChangedClassification(this.getClassification(sourceName, previousClassification, methodName));
                }
            }
        }
    }


    /**
     * Process an relationship extracted from an event.
     *
     * @param sourceName source of the event
     * @param eventType watchdog event type
     * @param relationship relationship from the event
     * @param previousRelationship previous value of the relationship
     * @param methodName calling method (indicates type of event action)
     */
    private void processWatchdogEvent(String            sourceName,
                                      WatchdogEventType eventType,
                                      Relationship      relationship,
                                      Relationship      previousRelationship,
                                      String            methodName)
    {
        if (relationship != null)
        {
            InstanceType type = relationship.getType();

            if (type != null)
            {
                WatchdogRelatedElementsEvent watchdogEvent = new WatchdogRelatedElementsEvent();

                watchdogEvent.setEventType(eventType);
                watchdogEvent.setRelatedMetadataElements(this.getRelatedElements(sourceName, relationship, methodName));

                if (previousRelationship != null)
                {
                    watchdogEvent.setPreviousRelatedMetadataElements(this.getRelatedElements(sourceName, previousRelationship, methodName));
                }
            }
        }
    }




    /**
     * A new entity has been created.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param entity                         details of the new entity
     */
    @Override
    public void processNewEntityEvent(String       sourceName,
                                      String       originatorMetadataCollectionId,
                                      String       originatorServerName,
                                      String       originatorServerType,
                                      String       originatorOrganizationName,
                                      EntityDetail entity)
    {
        final String methodName = "processNewEntityEvent";

        if ((! processGovernanceEngineEvent(sourceName, entity, methodName)) &&
                    (! processGovernanceActionEvent(sourceName, entity, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, entity)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.NEW_ELEMENT, entity, nullEntity, methodName);
        }
    }


    /**
     * An existing entity has been updated.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param oldEntity                      original values for the entity.
     * @param newEntity                      details of the new version of the entity.
     */
    @Override
    public void processUpdatedEntityEvent(String       sourceName,
                                          String       originatorMetadataCollectionId,
                                          String       originatorServerName,
                                          String       originatorServerType,
                                          String       originatorOrganizationName,
                                          EntityDetail oldEntity,
                                          EntityDetail newEntity)
    {
        final String methodName = "processUpdatedEntityEvent";

        if ((! processGovernanceEngineEvent(sourceName, newEntity, methodName)) &&
                    (! processGovernanceActionEvent(sourceName, newEntity, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, newEntity)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.UPDATED_ELEMENT_PROPERTIES, newEntity, oldEntity, methodName);
        }
    }


    /**
     * An update to an entity has been undone.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param entity                         details of the version of the entity that has been restored.
     */
    @Override
    public void processUndoneEntityEvent(String       sourceName,
                                         String       originatorMetadataCollectionId,
                                         String       originatorServerName,
                                         String       originatorServerType,
                                         String       originatorOrganizationName,
                                         EntityDetail entity)
    {
        final String methodName = "processUndoneEntityEvent";

        if ((! processGovernanceEngineEvent(sourceName, entity, methodName)) &&
                    (! processGovernanceActionEvent(sourceName, entity, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, entity)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.UPDATED_ELEMENT_PROPERTIES, entity, nullEntity, methodName);
        }
    }


    /**
     * A new classification has been added to an entity.
     *
     * @param sourceName  name of the source of the event.  It may be the cohort name for incoming events or the
     *                   local repository, or event mapper name.
     * @param originatorMetadataCollectionId  unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName  name of the server that the event came from.
     * @param originatorServerType  type of server that the event came from.
     * @param originatorOrganizationName  name of the organization that owns the server that sent the event.
     * @param entity  details of the entity with the new classification added. No guarantee this is all of the classifications.
     * @param classification new classification
     */
    @Override
    public void processClassifiedEntityEvent(String         sourceName,
                                             String         originatorMetadataCollectionId,
                                             String         originatorServerName,
                                             String         originatorServerType,
                                             String         originatorOrganizationName,
                                             EntityDetail   entity,
                                             Classification classification)
    {
        final String methodName = "processClassifiedEntityEvent";

        if ((! processGovernanceEngineEvent(sourceName, entity, methodName)) &&
                    (! processGovernanceActionEvent(sourceName, entity, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, entity)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.NEW_CLASSIFICATION, entity, classification, null, methodName);
        }
    }


    /**
     * A classification has been removed from an entity.
     *
     * @param sourceName  name of the source of the event.  It may be the cohort name for incoming events or the
     *                   local repository, or event mapper name.
     * @param originatorMetadataCollectionId  unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName  name of the server that the event came from.
     * @param originatorServerType  type of server that the event came from.
     * @param originatorOrganizationName  name of the organization that owns the server that sent the event.
     * @param entity  details of the entity after the classification has been removed. No guarantee this is all of the classifications.
     * @param originalClassification classification that was removed
     */
    @Override
    public void processDeclassifiedEntityEvent(String         sourceName,
                                               String         originatorMetadataCollectionId,
                                               String         originatorServerName,
                                               String         originatorServerType,
                                               String         originatorOrganizationName,
                                               EntityDetail   entity,
                                               Classification originalClassification)
    {
        final String methodName = "processDeclassifiedEntityEvent";

        if ((! processGovernanceEngineEvent(sourceName, entity, methodName)) &&
                    (! processGovernanceActionEvent(sourceName, entity, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, entity)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.DELETED_CLASSIFICATION, entity, originalClassification, null, methodName);
        }
    }


    /**
     * An existing classification has been changed on an entity.
     *
     * @param sourceName  name of the source of the event.  It may be the cohort name for incoming events or the
     *                   local repository, or event mapper name.
     * @param originatorMetadataCollectionId  unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName  name of the server that the event came from.
     * @param originatorServerType  type of server that the event came from.
     * @param originatorOrganizationName  name of the organization that owns the server that sent the event.
     * @param entity  details of the entity after the classification has been changed. No guarantee this is all of the classifications.
     * @param originalClassification classification that was removed
     * @param classification new classification
     */
    @Override
    public void processReclassifiedEntityEvent(String         sourceName,
                                               String         originatorMetadataCollectionId,
                                               String         originatorServerName,
                                               String         originatorServerType,
                                               String         originatorOrganizationName,
                                               EntityDetail   entity,
                                               Classification originalClassification,
                                               Classification classification)
    {
        final String methodName = "processReclassifiedEntityEvent";

        if ((! processGovernanceEngineEvent(sourceName, entity, methodName)) &&
                    (! processGovernanceActionEvent(sourceName, entity, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, entity)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.UPDATED_CLASSIFICATION_PROPERTIES, entity, classification, originalClassification, methodName);
        }
    }


    /**
     * An existing entity has been deleted.  This is a soft delete. This means it is still in the repository
     * but it is no longer returned on queries.
     * <p>
     * All relationships to the entity are also soft-deleted and will no longer be usable.  These deleted relationships
     * will be notified through separate events.
     * <p>
     * Details of the TypeDef are included with the entity's unique id (guid) to ensure the right entity is deleted in
     * the remote repositories.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param entity                         deleted entity
     */
    @Override
    public void processDeletedEntityEvent(String       sourceName,
                                          String       originatorMetadataCollectionId,
                                          String       originatorServerName,
                                          String       originatorServerType,
                                          String       originatorOrganizationName,
                                          EntityDetail entity)
    {
        final String methodName = "processDeletedEntityEvent";

        if ((! processGovernanceEngineEvent(sourceName, entity, methodName)) &&
                    (! processGovernanceActionEvent(sourceName, entity, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, entity)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.DELETED_ELEMENT, entity, nullEntity, methodName);
        }
    }


    /**
     * An existing entity has been deleted and purged in a single action.
     *
     * All relationships to the entity are also deleted and purged and will no longer be usable.  These deleted relationships
     * will be notified through separate events.
     *
     *
     * @param sourceName  name of the source of the event.  It may be the cohort name for incoming events or the
     *                   local repository, or event mapper name.
     * @param originatorMetadataCollectionId  unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName  name of the server that the event came from.
     * @param originatorServerType  type of server that the event came from.
     * @param originatorOrganizationName  name of the organization that owns the server that sent the event.
     * @param entity  deleted entity
     */
    @Override
    public void processDeletePurgedEntityEvent(String       sourceName,
                                               String       originatorMetadataCollectionId,
                                               String       originatorServerName,
                                               String       originatorServerType,
                                               String       originatorOrganizationName,
                                               EntityDetail entity)
    {
        final String methodName = "processDeletePurgedEntityEvent";

        if ((! processGovernanceEngineEvent(sourceName, entity, methodName)) &&
                    (! processGovernanceActionEvent(sourceName, entity, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, entity)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.DELETED_ELEMENT, entity, nullEntity, methodName);
        }
    }


    /**
     * A deleted entity has been restored to the state it was before it was deleted.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param entity                         details of the version of the entity that has been restored.
     */
    @Override
    public void processRestoredEntityEvent(String       sourceName,
                                           String       originatorMetadataCollectionId,
                                           String       originatorServerName,
                                           String       originatorServerType,
                                           String       originatorOrganizationName,
                                           EntityDetail entity)
    {
        final String methodName = "processRestoredEntityEvent";

        if ((! processGovernanceEngineEvent(sourceName, entity, methodName)) &&
                    (! processGovernanceActionEvent(sourceName, entity, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, entity)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.REFRESHED_ELEMENT, entity, nullEntity, methodName);
        }
    }


    /**
     * The guid of an existing entity has been changed to a new value.  This is used if two different
     * entities are discovered to have the same guid.  This is extremely unlikely but not impossible so
     * the open metadata protocol has provision for this.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param originalEntityGUID             the existing identifier for the entity.
     * @param entity                         new values for this entity, including the new guid.
     */
    @Override
    public void processReIdentifiedEntityEvent(String       sourceName,
                                               String       originatorMetadataCollectionId,
                                               String       originatorServerName,
                                               String       originatorServerType,
                                               String       originatorOrganizationName,
                                               String       originalEntityGUID,
                                               EntityDetail entity)
    {
        final String methodName = "processReIdentifiedEntityEvent";

        if ((! processGovernanceEngineEvent(sourceName, entity, methodName)) &&
                    (! processGovernanceActionEvent(sourceName, entity, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, entity)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.REFRESHED_ELEMENT, entity, nullEntity, methodName);
        }
    }


    /**
     * An existing entity has had its type changed.  Typically this action is taken to move an entity's
     * type to either a super type (so the subtype can be deleted) or a new subtype (so additional properties can be
     * added.)  However, the type can be changed to any compatible type.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param originalTypeDefSummary         original details of this entity's TypeDef.
     * @param entity                         new values for this entity, including the new type information.
     */
    @Override
    public void processReTypedEntityEvent(String         sourceName,
                                          String         originatorMetadataCollectionId,
                                          String         originatorServerName,
                                          String         originatorServerType,
                                          String         originatorOrganizationName,
                                          TypeDefSummary originalTypeDefSummary,
                                          EntityDetail   entity)
    {
        final String methodName = "processReTypedEntityEvent";

        if ((! processGovernanceEngineEvent(sourceName, entity, methodName)) &&
                    (! processGovernanceActionEvent(sourceName, entity, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, entity)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.REFRESHED_ELEMENT, entity, nullEntity, methodName);
        }
    }


    /**
     * An existing entity has changed home repository.  This action is taken for example, if a repository
     * becomes permanently unavailable, or if the user community updating this entity move to working
     * from a different repository in the open metadata repository cluster.
     *
     * @param sourceName                       name of the source of the event.  It may be the cohort name for incoming events or the
     *                                         local repository, or event mapper name.
     * @param originatorMetadataCollectionId   unique identifier for the metadata collection hosted by the server that
     *                                         sent the event.
     * @param originatorServerName             name of the server that the event came from.
     * @param originatorServerType             type of server that the event came from.
     * @param originatorOrganizationName       name of the organization that owns the server that sent the event.
     * @param originalHomeMetadataCollectionId unique identifier for the original home repository.
     * @param entity                           new values for this entity, including the new home information.
     */
    @Override
    public void processReHomedEntityEvent(String       sourceName,
                                          String       originatorMetadataCollectionId,
                                          String       originatorServerName,
                                          String       originatorServerType,
                                          String       originatorOrganizationName,
                                          String       originalHomeMetadataCollectionId,
                                          EntityDetail entity)
    {
        final String methodName = "processReHomedEntityEvent";

        if ((! processGovernanceEngineEvent(sourceName, entity, methodName)) &&
                    (! processGovernanceActionEvent(sourceName, entity, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, entity)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.REFRESHED_ELEMENT, entity, nullEntity, methodName);
        }
    }


    /**
     * A remote repository in the cohort has sent entity details in response to a refresh request.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param entity                         details of the requested entity
     */
    @Override
    public void processRefreshEntityEvent(String       sourceName,
                                          String       originatorMetadataCollectionId,
                                          String       originatorServerName,
                                          String       originatorServerType,
                                          String       originatorOrganizationName,
                                          EntityDetail entity)
    {
        final String methodName = "processRefreshEntityEvent";

        if ((! processGovernanceEngineEvent(sourceName, entity, methodName)) &&
                    (! processGovernanceActionEvent(sourceName, entity, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, entity)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.REFRESHED_ELEMENT, entity, nullEntity, methodName);
        }
    }


    /**
     * A new relationship has been created.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param relationship                   details of the new relationship
     */
    @Override
    public void processNewRelationshipEvent(String       sourceName,
                                            String       originatorMetadataCollectionId,
                                            String       originatorServerName,
                                            String       originatorServerType,
                                            String       originatorOrganizationName,
                                            Relationship relationship)
    {
        final String methodName = "processNewRelationshipEvent";

        if ((! processSupportedGovernanceService(sourceName, relationship, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, relationship)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.NEW_RELATIONSHIP, relationship, nullRelationship, methodName);
        }
    }


    /**
     * An existing relationship has been updated.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param oldRelationship                original details of the relationship.
     * @param newRelationship                details of the new version of the relationship.
     */
    @Override
    public void processUpdatedRelationshipEvent(String       sourceName,
                                                String       originatorMetadataCollectionId,
                                                String       originatorServerName,
                                                String       originatorServerType,
                                                String       originatorOrganizationName,
                                                Relationship oldRelationship,
                                                Relationship newRelationship)
    {
        final String methodName = "processUpdatedRelationshipEvent";

        if ((! processSupportedGovernanceService(sourceName, newRelationship, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, newRelationship)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.UPDATED_RELATIONSHIP_PROPERTIES, newRelationship, oldRelationship, methodName);
        }
    }


    /**
     * An update to a relationship has been undone.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param relationship                   details of the version of the relationship that has been restored.
     */
    @Override
    public void processUndoneRelationshipEvent(String       sourceName,
                                               String       originatorMetadataCollectionId,
                                               String       originatorServerName,
                                               String       originatorServerType,
                                               String       originatorOrganizationName,
                                               Relationship relationship)
    {
        final String methodName = "processUndoneRelationshipEvent";

        if ((! processSupportedGovernanceService(sourceName, relationship, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, relationship)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.REFRESHED_RELATIONSHIP, relationship, nullRelationship, methodName);
        }
    }


    /**
     * An existing relationship has been deleted.  This is a soft delete. This means it is still in the repository
     * but it is no longer returned on queries.
     * <p>
     * Details of the TypeDef are included with the relationship's unique id (guid) to ensure the right
     * relationship is deleted in the remote repositories.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param relationship                   deleted relationship
     */
    @Override
    public void processDeletedRelationshipEvent(String       sourceName,
                                                String       originatorMetadataCollectionId,
                                                String       originatorServerName,
                                                String       originatorServerType,
                                                String       originatorOrganizationName,
                                                Relationship relationship)
    {
        final String methodName = "processDeletedRelationshipEvent";

        if ((! processSupportedGovernanceService(sourceName, relationship, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, relationship)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.DELETED_RELATIONSHIP, relationship, nullRelationship, methodName);
        }
    }


    /**
     * An active relationship has been deleted and purged from the repository.  This request can not be undone.
     *
     * @param sourceName  name of the source of the event.  It may be the cohort name for incoming events or the
     *                   local repository, or event mapper name.
     * @param originatorMetadataCollectionId  unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName  name of the server that the event came from.
     * @param originatorServerType type of server that the event came from.
     * @param originatorOrganizationName  name of the organization that owns the server that sent the event.
     * @param relationship  deleted relationship
     */
    @Override
    public void processDeletePurgedRelationshipEvent(String       sourceName,
                                                     String       originatorMetadataCollectionId,
                                                     String       originatorServerName,
                                                     String       originatorServerType,
                                                     String       originatorOrganizationName,
                                                     Relationship relationship)
    {
        final String methodName = "processDeletePurgedRelationshipEvent";

        if ((! processSupportedGovernanceService(sourceName, relationship, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, relationship)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.DELETED_RELATIONSHIP, relationship, nullRelationship, methodName);
        }
    }


    /**
     * A deleted relationship has been restored to the state it was before it was deleted.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param relationship                   details of the version of the relationship that has been restored.
     */
    @Override
    public void processRestoredRelationshipEvent(String       sourceName,
                                                 String       originatorMetadataCollectionId,
                                                 String       originatorServerName,
                                                 String       originatorServerType,
                                                 String       originatorOrganizationName,
                                                 Relationship relationship)
    {
        final String methodName = "processRestoredRelationshipEvent";

        if ((! processSupportedGovernanceService(sourceName, relationship, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, relationship)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.REFRESHED_RELATIONSHIP, relationship, nullRelationship, methodName);
        }
    }


    /**
     * The guid of an existing relationship has changed.  This is used if two different
     * relationships are discovered to have the same guid.  This is extremely unlikely but not impossible so
     * the open metadata protocol has provision for this.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param originalRelationshipGUID       the existing identifier for the relationship.
     * @param relationship                   new values for this relationship, including the new guid.
     */
    @Override
    public void processReIdentifiedRelationshipEvent(String       sourceName,
                                                     String       originatorMetadataCollectionId,
                                                     String       originatorServerName,
                                                     String       originatorServerType,
                                                     String       originatorOrganizationName,
                                                     String       originalRelationshipGUID,
                                                     Relationship relationship)
    {
        final String methodName = "processReIdentifiedRelationshipEvent";

        if ((! processSupportedGovernanceService(sourceName, relationship, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, relationship)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.REFRESHED_RELATIONSHIP, relationship, nullRelationship, methodName);
        }
    }


    /**
     * An existing relationship has had its type changed.  Typically this action is taken to move a relationship's
     * type to either a super type (so the subtype can be deleted) or a new subtype (so additional properties can be
     * added.)  However, the type can be changed to any compatible type.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param originalTypeDefSummary         original details of this relationship's TypeDef.
     * @param relationship                   new values for this relationship, including the new type information.
     */
    @Override
    public void processReTypedRelationshipEvent(String         sourceName,
                                                String         originatorMetadataCollectionId,
                                                String         originatorServerName,
                                                String         originatorServerType,
                                                String         originatorOrganizationName,
                                                TypeDefSummary originalTypeDefSummary,
                                                Relationship   relationship)
    {
        final String methodName = "processReTypedRelationshipEvent";

        if ((! processSupportedGovernanceService(sourceName, relationship, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, relationship)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.REFRESHED_RELATIONSHIP, relationship, nullRelationship, methodName);
        }
    }


    /**
     * An existing relationship has changed home repository.  This action is taken for example, if a repository
     * becomes permanently unavailable, or if the user community updating this relationship move to working
     * from a different repository in the open metadata repository cluster.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param originalHomeMetadataCollection unique identifier for the original home repository.
     * @param relationship                   new values for this relationship, including the new home information.
     */
    @Override
    public void processReHomedRelationshipEvent(String       sourceName,
                                                String       originatorMetadataCollectionId,
                                                String       originatorServerName,
                                                String       originatorServerType,
                                                String       originatorOrganizationName,
                                                String       originalHomeMetadataCollection,
                                                Relationship relationship)
    {
        final String methodName = "processReHomedRelationshipEvent";

        if ((! processSupportedGovernanceService(sourceName, relationship, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, relationship)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.REFRESHED_RELATIONSHIP, relationship, nullRelationship, methodName);
        }
    }


    /**
     * The local repository is refreshing the information about a relationship for the other
     * repositories in the cohort.
     *
     * @param sourceName                     name of the source of the event.  It may be the cohort name for incoming events or the
     *                                       local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName           name of the server that the event came from.
     * @param originatorServerType           type of server that the event came from.
     * @param originatorOrganizationName     name of the organization that owns the server that sent the event.
     * @param relationship                   relationship details
     */
    @Override
    public void processRefreshRelationshipEvent(String       sourceName,
                                                String       originatorMetadataCollectionId,
                                                String       originatorServerName,
                                                String       originatorServerType,
                                                String       originatorOrganizationName,
                                                Relationship relationship)
    {
        final String methodName = "processRefreshRelationshipEvent";

        if ((! processSupportedGovernanceService(sourceName, relationship, methodName)) &&
                    (! excludeGovernanceManagementEvents(sourceName, relationship)))
        {
            processWatchdogEvent(sourceName, WatchdogEventType.REFRESHED_RELATIONSHIP, relationship, nullRelationship, methodName);
        }
    }


    /**
     * An open metadata repository is passing information about a collection of entities and relationships
     * with the other repositories in the cohort.
     *
     * @param sourceName name of the source of the event.  It may be the cohort name for incoming events or the
     *                   local repository, or event mapper name.
     * @param originatorMetadataCollectionId unique identifier for the metadata collection hosted by the server that
     *                                       sent the event.
     * @param originatorServerName name of the server that the event came from.
     * @param originatorServerType type of server that the event came from.
     * @param originatorOrganizationName name of the organization that owns the server that sent the event.
     * @param instances multiple entities and relationships for sharing.
     */
    @Override
    public void processInstanceBatchEvent(String        sourceName,
                                          String        originatorMetadataCollectionId,
                                          String        originatorServerName,
                                          String        originatorServerType,
                                          String        originatorOrganizationName,
                                          InstanceGraph instances)
    {
        final String methodName = "processInstanceBatchEvent";

        if (instances != null)
        {
            List<EntityDetail> entities = instances.getEntities();

            if (entities != null)
            {
                for (EntityDetail entity : entities)
                {
                    processGovernanceEngineEvent(sourceName, entity, methodName);
                }
            }

            List<Relationship>  relationships = instances.getRelationships();

            if (relationships != null)
            {
                for (Relationship relationship : relationships)
                {
                    processSupportedGovernanceService(sourceName, relationship, methodName);
                }
            }
        }
    }
}
