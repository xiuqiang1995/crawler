package com.github;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;


public class Main {
    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:/Users/br/githubws/crawler-1/crawlerDB", "root", "root");
        while (true) {
            //待处理链接池
            List<String> linkPool = loadLinksFromDataBase(connection, "select link from LINKS_TO_BE_PROCESSED");
            if (linkPool.isEmpty()) {
                break;
            }
            //删除将要被处理的链接
            String link = linkPool.remove(linkPool.size() - 1);
            insertLinkIntoDataBase(connection, link, "delete from LINKS_TO_BE_PROCESSED where link = ?");

            //断点续传
            if (!isLinkProcessed(connection, link)) {
                if (isNewsLink(link)) {
                    Document doc = HttpGetAndParseHtml(link);
                    parseLinkAndStoreIntoDataBase(connection, doc);
                    storeIntoDataBase(doc);
                    insertLinkIntoDataBase(connection, link, "INSERT INTO LINKS_ALREADY_PROCESSED (link) values (?)");
                }
            }

        }
    }

    private static void parseLinkAndStoreIntoDataBase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            //将爬到的LINK存入 LINKS_TO_BE_PROCESSED 表中
            insertLinkIntoDataBase(connection, href, "INSERT INTO LINKS_TO_BE_PROCESSED (link) values (?)");
        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement ps = connection.prepareStatement("select link from LINKS_ALREADY_PROCESSED where link = ?")) {
            ps.setString(1, link);
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                //处理过
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }

    //将处理过的link加入到 LINKS_ALREADY_PROCESSED 表中
    private static void insertLinkIntoDataBase(Connection connection, String href, String s) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(s)) {
            ps.setString(1, href);
            ps.executeUpdate();
        }
    }

    private static List<String> loadLinksFromDataBase(Connection connection, String sql) throws SQLException {
        ResultSet resultSet = null;
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return list;
    }

    private static void storeIntoDataBase(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            String title = articleTags.get(0).child(0).text();
            System.out.println("title = " + title);
        }
    }

    private static Document HttpGetAndParseHtml(String link) throws IOException {
        //从官方文档抄代码 http://hc.apache.org/httpcomponents-client-4.5.x/quickstart.html
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
        }
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        Document doc = null;
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);
            doc = Jsoup.parse(html);

        }
        return doc;
    }

    private static boolean isNewsLink(String link) {
        //is Index Page ?
        if ("https://sina.cn".equals(link)) {
            return true;
        }
        //is news page and is not login page
        return link.contains("news.sina.cn") && !link.contains("passport.sina.cn");
    }
}
