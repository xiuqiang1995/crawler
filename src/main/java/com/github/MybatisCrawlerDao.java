package com.github;

import java.util.HashMap;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;

public class MybatisCrawlerDao implements CrawlerDao {
    private SqlSessionFactory sqlSessionFactory;

    public MybatisCrawlerDao() {
        String resource = "db/mybatis/config.xml";
        InputStream inputStream = null;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return (Integer) session.selectOne("com.github.MyMapper.countLink", link) > 0;
        }
    }

    @Override
    public void insertNewsIntoDatabase(String link, String title, String content) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.MyMapper.insertNews", new News(link, title, content));
        }
    }

    @Override
    public String getOneLinkAndDeleteIt() throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String one = session.selectOne("com.github.MyMapper.selectNextAvailableLink");
            if (one != null) {
                session.delete("com.github.MyMapper.deleteLink", one);
            }
            return one;
        }
    }

    @Override
    public void insertLinkToBeProcessed(String href) {
        insertLinkIntoDataBase(href, "links_to_be_processed");
    }

    @Override
    public void insertProcessedLink(String link) {
        insertLinkIntoDataBase(link, "links_already_processed");
    }

    private void insertLinkIntoDataBase(String href, String tableName) {
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableName);
        map.put("link", href);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.MyMapper.insertLink", map);
        }
    }
}
