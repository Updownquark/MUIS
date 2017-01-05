<?xml version="1.0" encoding="UTF-8"?>

<quick>
	<head>
		<title>Kids Timer</title>
		<style-sheet ref="../styles/quick-tests.qss"></style-sheet>
		<model name="model" builder="default-model">
			<model name="counter" builder="counter-model" min="0" max="3000" rate="100mi" loop="false" max-frequency="100mi" start="false" />
			<value name="start" type="java.time.Duration">${this.counter.max*100}</value>
			<value name="remainValue">this.counter.max-this.counter.value</value>
			<value name="remaining" type="java.time.Duration">${this.remainValue*100}</value>

			<value name="hourLength">36000</value>
			<switch name="hourColorValue" value="this.remainValue>this.hourLength">
				<case value="true">(this.remainValue-this.hourLength)*1.0/(this.counter.max-this.hourLength)</case>
				<default>0</default>
			</switch>
			<value name="hourColor" type="java.awt.Color">${rgb((int)(this.hourColorValue*255), (int)((1-this.hourColorValue)*255), 0)}</value>

			<value name="fifteenMinuteLength">15*600</value>
			<switch name="fifteenMinuteMax" value="this.counter.max>=this.hourLength">
				<case value="true">this.hourLength</case>
				<default>this.counter.max</default>
			</switch>
			<switch name="fifteenMinuteColorValue" value="this.remainValue&lt;=this.fifteenMinuteLength ? -1 : (this.remainValue>this.fifteenMinuteMax ? 1 : 0)">
				<case value="0">(this.remainValue-this.fifteenMinuteLength)*1.0/(this.fifteenMinuteMax-this.fifteenMinuteLength)</case>
				<case value="1">1</case>
				<default>0</default>
			</switch>
			<value name="fifteenMinuteColor" type="java.awt.Color">${rgb((int)(this.fifteenMinuteColorValue*255), (int)((1-this.fifteenMinuteColorValue)*255), 0)}</value>

			<value name="minuteLength">600</value>
			<switch name="minuteMax" value="this.counter.max>=this.fifteenMinuteLength">
				<case value="true">this.fifteenMinuteLength</case>
				<default>this.counter.max</default>
			</switch>
			<switch name="minuteColorValue" value="this.remainValue&lt;=this.minuteLength ? -1 : (this.remainValue>this.minuteMax ? 1 : 0)">
				<case value="0">(this.remainValue-this.minuteLength)*1.0/(this.minuteMax-this.minuteLength)</case>
				<case value="1">1</case>
				<default>0</default>
			</switch>
			<value name="minuteColor" type="java.awt.Color">${rgb((int) (this.minuteColorValue*255), (int) ((1-this.minuteColorValue)*255), 0)}</value>

			<value name="secondLength">600</value>
			<switch name="secondMax" value="this.counter.max>=this.minuteLength">
				<case value="true">this.minuteLength</case>
				<default>this.counter.max</default>
			</switch>
			<switch name="secondColorValue" value="this.remainValue&lt;=this.secondMax">
				<case value="true">this.remainValue*1.0/this.secondMax</case>
				<default>1</default>
			</switch>
			<value name="secondColor" type="java.awt.Color">${rgb((int) (this.secondColorValue*255), (int) ((1-this.secondColorValue)*255), 0)}</value>
		</model>
	</head>
	<body xmlns:base="../../../../base/QuickRegistry.xml" layout="box" direction="down" cross-align="center">
		<spinner value="model.start" format-factory="${formats.durationHHMMss}" rich="true" length="8" style="font.size=40" />
		<block layout="box" style="layout.padding=0">
			<label group="timer-display" value="model.remaining" format-factory="${formats.durationHHMMss}" />
			<label group="timer-display">.</label>
			<label group="timer-display" value="(model.counter.max-model.counter.value)%10" />
		</block>
		<block layout="box">
    		<toggle-button selected="model.counter.running">
    			<label value="model.counter.running ? &quot;Pause&quot; : (model.counter.value==model.counter.min ? &quot;Start&quot; : &quot;Resume&quot;)" />
    		</toggle-button>
			<button action="model.counter.running=false;model.counter.value=model.counter.min">Stop</button>
		</block>
		<block layout="box" align="justify" width="100%">
			<block width="20%" height="150" style="bg.transparency=0;bg.color=${model.hourColor}" />
			<block width="20%" height="150" style="bg.transparency=0;bg.color=${model.fifteenMinuteColor}" />
			<block width="20%" height="150" style="bg.transparency=0;bg.color=${model.minuteColor}" />
			<block width="20%" height="150" style="bg.transparency=0;bg.color=${model.secondColor}" />
		</block>
	</body>
</quick>
