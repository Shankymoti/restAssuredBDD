package com.shashankkumar.core;

//import com.shashankkumar.Utils.DataTimeUtils;
//import com.shashankkumar.Utils.PropertiesUtil;
//import com.shashankkumar.Utils.rest.ScenarioContext;
import io.cucumber.core.logging.Logger;
import io.cucumber.core.logging.LoggerFactory;
import  io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
//import org.apache.commons.io.FileUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

import java.util.regex.Pattern;

/**
 @author shashank.kumar
 */

@CucumberOptions(plugin = {"pretty", "com.shashank.reporter.BDDReportListner", "json:target/cucumber.json",
        "html:test-output/cucumber-reports.html","pretty","io.qameta.allure.cucumber7jum.AllureCucumber7Jum"})
public class BaseReunner extends AbstractTestNGCucumberTests{
private static final Logger log = LoggerFactory.getLogger(BaseReunner.class);
private static final Pattern pattern = Pattern.compile("^chrome_.*$");


//private static ThreadLocal<ApiContext> context =  new ThreadLocal<ApiContext>();



}
