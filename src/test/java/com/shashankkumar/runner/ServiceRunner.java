package com.shashankkumar.runner;

import io.cucumber.testng.CucumberOptions;
import io.cucumber.testng.CucumberOptions.SnippetType;
import org.testng.annotations.DataProvider;



@CucumberOptions(features = "classpath:features", dryRun = false, snippets = CucumberOptions.SnippetType.CAMELCASE,
        glue = {"com.shashankkumar.stepdefinitions"}, monochrome = true, tags = "@TC" )
public class ServiceRunner extends BaseRunner {

    @DataProvider(parallel = true)
    @Override
    public Object[][] scenarios()
    {
        return  super.scenarios();
    }    
    
    
}
