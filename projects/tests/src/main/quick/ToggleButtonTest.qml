<?xml version="1.0" encoding="UTF-8"?>

<quick xmlns:base="../../../../base/QuickRegistry.xml" xmlns:test="../../../QuickRegistry.xml">
    <head>
        <title>Testing Buttons</title>
		<style-sheet ref="../styles/quick-tests.qss"></style-sheet>
        <model name="model" builder="default-model">
        	<variable name="value">0</variable>
        	<switch name="colorGroup" value="this.value%4">
        		<case value="0">&quot;bg-red&quot;</case>
        		<case value="1">&quot;bg-blue&quot;</case>
        		<case value="2">&quot;bg-green&quot;</case>
        		<case value="3">&quot;bg-purple&quot;</case>
        	</switch>
        	<value name="red">this.value==0</value>
        	<value name="blue">this.value==1</value>
        	<value name="green">this.value==2</value>
        	<value name="purple">this.value==3</value>
        </model>
    </head>
    <body layout="simple">
    	<block layout="simple" width="100%" top="0" height="100%">
    		Choose a background color:
    		<toggle-button left="10" top="20px" height="30px" selected="model.red">Red</toggle-button>
    		<toggle-button left="10" top="50px" height="30px" selected="model.blue">Blue</toggle-button>
    		<toggle-button left="10" top="80px" height="30px" selected="model.green">Green</toggle-button>
    		<toggle-button left="10" top="110px" height="30px" selected="model.purple">Purple</toggle-button>
    		<label group="#{${model.colorGroup}}" value="model.colorGroup" style="font.size=18;font.weight=bold" right="10xp" top="60" />
    	</block>
    </body>
</quick>
