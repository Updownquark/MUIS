<?xml version="1.0" encoding="UTF-8"?>

<quick xmlns:test="../../../QuickRegistry.xml">
    <head>
        <title>Model Test</title>
		<style-sheet ref="../styles/quick-tests.qss"></style-sheet>
        <model name="model" builder="default-builder">
        	<variable name="colorIndex">0</variable>
        	<model name="bg">
        		<value name="red">this.colorIndex==0</value>
        		<value name="blue">this.colorIndex==1</value>
        		<value name="green">this.colorIndex==2</value>
        		<value name="image">org.quick.test.model.ModelTestModel.getBgImage(this.colorIndex)</value>
        		<value name="groupName">org.quick.test.model.ModelTestModel.getGroupName(this.colorIndex)</value>
        	</model>
        	<model name="fg">
        		<value name="value">org.quick.test.model.ModelTestModel.getFgColor(this.colorIndex)</value>
        		<value name="text">org.quick.test.model.ModelTestModel.getText(this.colorIndex)</value>
        	</model>
        </model>
    </head>
    <body xmlns:base="../../../../base/QuickRegistry.xml">
   		<block layout="base:border-layout">
   			<label region="top" height="30" style="font.size=24">Model Test</label>
   			<block region="left" width="120" layout="box" direction="down" cross-align="justify">
   				<label style="font.size=16">Choose Background</label>
	    		<toggle-button value="model.bg.red">Red</toggle-button>
	    		<toggle-button value="model.bg.blue">Blue</toggle-button>
	    		<toggle-button value="model.bg.green">Green</toggle-button>
	    		<image src="test:model.bg.image" prop-locked="true" resize="resize" width="100%"/>
   			</block>
   			<block region="right" width="200" layout="box" direction="down">
	   			<text-field value="model.fg.text" length="30"></text-field>
   			</block>
   			<border region="center" group="model.bg.groupName" style="font.color=model.fg.value">
   				This text's background should match the selected button at left.
   				Its font color should match the value at right.
   			</border>
   		</block>
    </body>
</quick>
