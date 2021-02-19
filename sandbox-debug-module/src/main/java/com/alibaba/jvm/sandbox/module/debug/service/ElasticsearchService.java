package com.alibaba.jvm.sandbox.module.debug.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * es数据库服务
 */
public class ElasticsearchService {
    private RestHighLevelClient client;
    private BatchInsert batchInsertRun;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final Logger logger = LoggerFactory.getLogger("DEBUG-ELASTICSEARCH-LOGGER");

    public ElasticsearchService(String esHost, Integer esPort) {
        batchInsertRun = new BatchInsert();
        try {
            client = new RestHighLevelClient(RestClient.builder(new HttpHost(esHost, esPort, "http")));

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "Elasticsearch-thread-pool");
                }
            });
            scheduler.scheduleAtFixedRate(batchInsertRun, 10, 10, SECONDS);
        } catch (Exception e) {
            logger.info("Elasticsearch连接异常", e);

            client = null;
        }
    }

    /**
     * 批量入库runnable
     */
    private class BatchInsert implements Runnable {
        private LinkedBlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<Map<String, Object>>(10000);

        @Override
        public void run() {
            ArrayList<Map<String, Object>> eventList = new ArrayList<Map<String, Object>>();
            queue.drainTo(eventList);

            insertData2ES(eventList);
        }

        /**
         * 保存数据
         *
         * @param eventList
         */
        private void insertData2ES(List<Map<String, Object>> eventList) {
            try {
                if (eventList.size() > 0) {
                    BulkRequest bulkRequest = new BulkRequest();
                    for (Map<String, Object> event : eventList) {
                        Object indexName = event.get("index_name");

                        IndexRequest request = new IndexRequest(indexName.toString());
                        request.id(UUID.randomUUID().toString());
                        request.source(event);

                        bulkRequest.add(request);
                    }

                    BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                    int status = bulkResponse.status().getStatus();

                    String todayStr = sdf.format(new Date());
                    if (status == 200) {
                        logger.info("ES批次入库记录数: " + eventList.size() + ",入库时间:" + todayStr);
                    } else {
                        logger.info("ES入库异常: " + status + ",当前时间:" + todayStr);
                    }
                }
            } catch (Exception e) {
                logger.info("数据入库异常: " + e.getMessage());
            }
        }

        /**
         * 缓存数据
         *
         * @param event
         */
        public void cache(Map<String, Object> event) {
            try {
                queue.add(event);
            } catch (IllegalStateException e) {
                List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                queue.drainTo(list);
                if (CollectionUtils.isNotEmpty(list)) {
                    insertData2ES(list);
                }

                queue.add(event);
            } catch (Exception e) {
                logger.info("es缓存异常: " + e.getMessage());
            }
        }
    }

    public void sink(Map<String, Object> event) {
        batchInsertRun.cache(event);
    }
}
