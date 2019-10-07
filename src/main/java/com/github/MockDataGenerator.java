package com.github;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class MockDataGenerator {
    private static void mockData(SqlSessionFactory sqlSessionFactory, int howmany) {
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            List<News> news = session.selectList("com.github.MockMapper.selectNews");
            int count = howmany - news.size();
            Random random = new Random();
            try {
                while (count-- > 0) {
                    int index = random.nextInt(news.size());
                    News newsToBeInserted = news.get(index);

                    Instant curr = newsToBeInserted.getCreatedAt();
                    curr.minusSeconds(random.nextInt(3600 * 24));
                    newsToBeInserted.setCreatedAt(curr);
                    newsToBeInserted.setModifiedAt(curr);

                    session.insert("com.github.MockMapper.insertNews", newsToBeInserted);
                    System.out.println("count = " + count);
                    if (count % 2000 == 0) {
                        session.flushStatements();
                    }
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory = null;
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mockData(sqlSessionFactory, 1000000);
    }
}
