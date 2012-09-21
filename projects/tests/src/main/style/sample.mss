
//A sample style sheet for MUIS.  Just trying stuff out here.

[base:block]
{
	bg.texture=base:raised-round
//	bg={
//		color=blue
//		corner-radius=5
//	}
	.hovered{
		color=yellow
	}
}
[base:block].clicked{
	bg.color=purple
}
(group){
	bg.color=green
}
(group)[base:button, base:block]{
	.clicked{
		bg.color=orange
	}
}
[body](group){
	bg.color=gray
}
