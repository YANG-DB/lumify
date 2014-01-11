package com.altamiracorp.lumify.model.accumuloqueue;

import backtype.storm.topology.IRichSpout;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.model.accumuloqueue.model.QueueItemRepository;
import com.google.inject.Inject;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BigTableWorkQueueRepository extends WorkQueueRepository {
    public static final String DEFAULT_TABLE_PREFIX = "atc_accumuloqueue_";
    private ModelSession modelSession;
    private Map<String, Boolean> queues = new HashMap<String, Boolean>();
    private String tablePrefix;
    private User user;
    private QueueItemRepository queueItemRepository;

    @Override
    public void init(Map config) {
        super.init(config);

        this.tablePrefix = (String) config.get(Configuration.WORK_QUEUE_REPOSITORY + ".tableprefix");
        if (this.tablePrefix == null) {
            this.tablePrefix = DEFAULT_TABLE_PREFIX;
        }
    }

    @Override
    public IRichSpout createSpout(Configuration configuration, String queueName, Long queueStartOffsetTime) {
        return new BigtableWorkQueueSpout(configuration, queueName);
    }

    @Override
    public void pushOnQueue(String queueName, JSONObject json, String... extra) {
        String tableName = getTableName(this.tablePrefix, queueName);

        if (this.user == null) {
            this.user = new SystemUser();
        }
        if (this.queueItemRepository == null) {
            this.queueItemRepository = new QueueItemRepository(this.modelSession, tableName);
        }

        if (!this.queues.containsKey(queueName)) {
            this.modelSession.initializeTable(tableName, this.user.getModelUserContext());
            this.queues.put(queueName, true);
        }

        this.queueItemRepository.add(json, extra, user);
    }

    static String getTableName(String tablePrefix, String queueName) {
        return tablePrefix + queueName;
    }

    @Inject
    public void setModelSession(ModelSession modelSession) {
        this.modelSession = modelSession;
    }
}
