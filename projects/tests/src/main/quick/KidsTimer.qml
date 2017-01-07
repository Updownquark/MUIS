<?xml version="1.0" encoding="UTF-8"?>

<quick>
	<head>
		<title>Kids Timer</title>
		<style-sheet ref="../styles/quick-tests.qss"></style-sheet>
		<model name="model" builder="default-model">
			<model name="timer" builder="counter-model" min="0" max="3000" rate="100mi" on-finish="stop" max-frequency="100mi" start="false" />
			<value name="start" type="java.time.Duration">${this.timer.max*100}</value>
			<value name="remainValue">this.timer.max-this.timer.value</value>
			<value name="remaining" type="java.time.Duration">${this.remainValue*100}</value>

			<value name="hourLength">36000</value>
			<switch name="hourColorValue" value="this.remainValue>this.hourLength">
				<case value="true">(this.remainValue-this.hourLength)*1.0/(this.timer.max-this.hourLength)</case>
				<default>0</default>
			</switch>
			<value name="hourColor" type="java.awt.Color">${rgb((int)(this.hourColorValue*255), (int)((1-this.hourColorValue)*255), 0)}</value>

			<value name="fifteenMinuteLength">15*600</value>
			<switch name="fifteenMinuteMax" value="this.timer.max>=this.hourLength">
				<case value="true">this.hourLength</case>
				<default>this.timer.max</default>
			</switch>
			<switch name="fifteenMinuteColorValue" value="this.remainValue&lt;=this.fifteenMinuteLength ? -1 : (this.remainValue>this.fifteenMinuteMax ? 1 : 0)">
				<case value="0">(this.remainValue-this.fifteenMinuteLength)*1.0/(this.fifteenMinuteMax-this.fifteenMinuteLength)</case>
				<case value="1">1</case>
				<default>0</default>
			</switch>
			<value name="fifteenMinuteColor" type="java.awt.Color">${rgb((int)(this.fifteenMinuteColorValue*255), (int)((1-this.fifteenMinuteColorValue)*255), 0)}</value>

			<value name="minuteLength">600</value>
			<switch name="minuteMax" value="this.timer.max>=this.fifteenMinuteLength">
				<case value="true">this.fifteenMinuteLength</case>
				<default>this.timer.max</default>
			</switch>
			<switch name="minuteColorValue" value="this.remainValue&lt;=this.minuteLength ? -1 : (this.remainValue>this.minuteMax ? 1 : 0)">
				<case value="0">(this.remainValue-this.minuteLength)*1.0/(this.minuteMax-this.minuteLength)</case>
				<case value="1">1</case>
				<default>0</default>
			</switch>
			<value name="minuteColor" type="java.awt.Color">${rgb((int) (this.minuteColorValue*255), (int) ((1-this.minuteColorValue)*255), 0)}</value>

			<value name="secondLength">600</value>
			<switch name="secondMax" value="this.timer.max>=this.minuteLength">
				<case value="true">this.minuteLength</case>
				<default>this.timer.max</default>
			</switch>
			<switch name="secondColorValue" value="this.remainValue&lt;=this.secondMax">
				<case value="true">this.remainValue*1.0/this.secondMax</case>
				<default>1</default>
			</switch>
			<value name="secondColor" type="java.awt.Color">${rgb((int) (this.secondColorValue*255), (int) ((1-this.secondColorValue)*255), 0)}</value>
			
			<model name="flash" builder="counter-model" min="0" max="9" rate="333mi" max-frequency="50mi" on-finish="reset" start="false" />
			<on-event event="this.timer.loop">this.flash.running=true</on-event>
			<value name="flashColor" type="java.awt.Color">${this.flash.value%2==0 ? red : blue}</value>
		</model>
	</head>
	<body xmlns:base="../../../../base/QuickRegistry.xml">
		<block layout="box" direction="down" cross-align="center"
				style="bg.transparency=model.flash.running ? 0 : 1;
					bg.color=${model.flashColor}">
			<spinner value="model.start" format-factory="${formats.durationHHMMss}" rich="true" length="8"
					style="font.size=40;font.weight=bold" />
			<block layout="box" style="layout.padding=0">
				<label group="timer-display" value="model.remaining" format-factory="${formats.durationHHMMss}" />
				<label group="timer-display">.</label>
				<label group="timer-display" value="(model.timer.max-model.timer.value)%10" />
			</block>
			<block layout="box">
	    		<toggle-button selected="model.timer.running">
	    			<label value="model.timer.running ?
	    					&quot;Pause&quot; :
	    					(model.timer.value==model.timer.min ?
	    						&quot;Start&quot; :
	    						&quot;Resume&quot;)" />
	    		</toggle-button>
				<button action="model.timer.running=false;model.timer.value=model.timer.min">Stop</button>
				<button action="model.timer.value=model.timer.min">Reset</button>
			</block>
			<block layout="box" align="justify" width="100%">
				<block width="20%" height="150" style="bg.transparency=0;bg.color=${model.hourColor}" />
				<block width="20%" height="150" style="bg.transparency=0;bg.color=${model.fifteenMinuteColor}" />
				<block width="20%" height="150" style="bg.transparency=0;bg.color=${model.minuteColor}" />
				<block width="20%" height="150" style="bg.transparency=0;bg.color=${model.secondColor}" />
			</block>
		</block>
	</body>
</quick>
