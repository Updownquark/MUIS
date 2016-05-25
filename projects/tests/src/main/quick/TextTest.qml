<?xml version="1.0" encoding="UTF-8"?>

<quick>
    <head>
        <title>Text Test</title>
    </head>
    <body xmlns:base="../../../../base/QuickRegistry.xml" xmlns:test="../../../QuickRegistry.xml">
   		<block layout="base:box" style="font.family=Arial" direction="down">
   			<label>Arial Base</label>
   			<label style="font.transparency=0.5;font.size=20">BIG (and transparent)</label>
   			<label style="font.size=8">small</label>
   			<label style="font.weight=light">Light</label>
   			<label style="font.color=yellow;font.weight=bold">Bold (and yellow)</label>
   			<label style="font.color=red;font.weight=ultra-bold">Ultra Bold (and red)</label>
   			<label style="font.slant=italic">Italic</label>
   			<label style="font.slant=-.75">Back Slant (Not always supported)</label>
   			<label style="font.strike=true">Strikethrough!</label>
   			<label style="font.underline=dashed">Dashed Underline!</label>
   			<label rich="true" style="font.size=16">This is {red}{b}rich{/red}{/b} {ul}formatted{/ul}
   				{bg.transparency=0;bg.color=red;font.color=blue}text{/bg.transparency;bg.color}{/blue}.</label>
   		</block>
    </body>
</quick>
