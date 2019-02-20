<?xml version="1.0" encoding="UTF-8"?>

<quick>
	<head>
		<title>Quick Table Test</title>
		<model name="model" builder="default-model">
			<variable name="rows">
				org.observe.collect.ObservableCollection.create(org.observe.util.TypeTokens.STRING,
					new org.qommons.tree.SortedTreeList&lt;String&gt;(true, org.qommons.QommonsUtils.DISTINCT_NUMBER_TOLERANT))
			</variable>
		</model>
	</head>
	<body layout="base:simple" xmlns:base="../../../../base/QuickRegistry.xml">
		<!-- TODO Enclose this table in a scroll pane when such a thing exists -->
		<table rows="rows" row-value="row">
			<table-column>
				<label role="renderer" value="row" />
				<text-field role="editor" value="row" />
			</table-column>
			<table-column>
				<label role="renderer" value="org.quick.test.util.QuickTestUtils.reverse(row)" />
			</table-column>
			<table-column>
				<label role="renderer" value="row.hashCode()" />
			</table-column>
		</table>
	</body>
</quick>
