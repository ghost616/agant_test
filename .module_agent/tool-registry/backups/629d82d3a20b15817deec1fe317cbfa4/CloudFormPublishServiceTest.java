package com.datanew.lcz.cloudform.service;

import com.datanew.core.sys.BaseApplication;
import com.datanew.core.sys.GlobalVariable;
import com.datanew.creater.data.DbContext;
import com.datanew.creater.data.DbContextConfig;
import com.mysql.jdbc.Driver;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.DriverManager;
import java.sql.SQLException;

public class CloudFormPublishServiceTest {

    @BeforeClass
    public static void start() throws SQLException {
        BaseApplication.getBase().getBaseVariable().initialize();
        DriverManager.registerDriver(new Driver());

    }

    @Test
    public void getDbContext() {

    }

    @Test
    public void getCustomForm() {

    }

    @Test
    public void generateDbSchema() {


    }

    @Test
    public void writeTemplate() {
        DbContext context = DbContextConfig.create("Hello", "MySql", "MySql",
                "jdbc:mysql://144.20.80.146:3306/creater系统", "root",
                "123456", "com.mysql.jdbc.Driver").createDbContext();

    }

    @Test
    public void writeTemplateNameToDb() {
    }
}