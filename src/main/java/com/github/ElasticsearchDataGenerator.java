package com.github;

import org.apache.http.HttpHost;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticsearchDataGenerator {
    public static void main(String[] args) throws IOException {
        SqlSessionFactory sqlSessionFactory = null;
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<News> newsFromMySQL = getNewsFromMySQL(sqlSessionFactory);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> writeIntoElasticWithThread(newsFromMySQL)).start();
        }
    }

    private static void writeIntoElasticWithThread(List<News> newsFromMySQL) {
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")))) {
            for (int i = 0; i < 1000; i++) {
                BulkRequest bulkRequest = new BulkRequest();
                for (News news : newsFromMySQL) {
                    //https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.4/_changing_the_application_8217_s_code.html
                    IndexRequest request = new IndexRequest("news");
                    Map<String, Object> data = new HashMap<>();
                    //截图内容,减小磁盘压力
                    data.put("content", news.getContent().length() > 10 ? news.getContent().substring(0, 10) : news.getContent());
                    data.put("url", news.getUrl());
                    data.put("title", news.getTitle());
                    data.put("createdAt", news.getCreatedAt());
                    data.put("modifiedAt", news.getModifiedAt());

                    request.source(data, XContentType.JSON);
                    bulkRequest.add(request);
                }
                BulkResponse bulk = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                System.out.println("current thread:" + Thread.currentThread().getName() + " finishes " + i + ":" + bulk.status().getStatus());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<News> getNewsFromMySQL(SqlSessionFactory sqlSessionFactory) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            List<News> objects = session.selectList("com.github.MockMapper.selectNews");
            return objects;
        }
    }
}
