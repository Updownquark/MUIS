<?xml version="1.0" encoding="UTF-8"?>

<quick xmlns:base="../../../../base/QuickRegistry.xml" xmlns:test="../../../QuickRegistry.xml">
    <head>
        <title>Testing Buttons</title>
        <model name="model" class="org.quick.test.model.ButtonTestModel" />
    </head>
    <body layout="base:simple">
    	<block layout="simple" width="100%" top="0" height="100%">
    		<label left="3" right="100%" top="0" height="25%">This is a MUIS Document</label>
			<button left="0" right="100%" top="25%" height="75%" action="model.clicked">
				The first set of Content is short
				<label style="font.color=red;font.weight=bold;bg.color=purple">The second set of Content is a bit longer than the first set</label>
			</button>
    	</block>
    </body>
</quick>
