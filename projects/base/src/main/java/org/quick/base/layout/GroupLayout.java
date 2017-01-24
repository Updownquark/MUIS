package org.quick.base.layout;

import java.util.*;

import org.observe.Observable;
import org.observe.ObservableValue;
import org.observe.assoc.ObservableTree;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableList;
import org.quick.core.QuickElement;
import org.quick.core.QuickLayout;
import org.quick.core.layout.*;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.style.LayoutStyle;

import com.google.common.reflect.TypeToken;

public class GroupLayout implements QuickLayout {
	public static final QuickAttribute<String> H_GROUP = QuickAttribute.build("h-group", QuickPropertyType.string).build();
	public static final QuickAttribute<String> V_GROUP = QuickAttribute.build("v-group", QuickPropertyType.string).build();

	private static List<QuickAttribute<?>> CHILD_ATTRIBUTES = Collections.unmodifiableList(Arrays.asList(//
		LayoutAttributes.minWidth, LayoutAttributes.width, LayoutAttributes.maxWidth, //
		LayoutAttributes.minHeight, LayoutAttributes.height, LayoutAttributes.maxHeight, //
		LayoutAttributes.alignment, LayoutAttributes.crossAlignment));

	@Override
	public void install(QuickElement parent, Observable<?> until) {
		/* Parent must specify the direction attribute and uses margin and padding
		 * Layout sub-groups must also specify the direction attributes (enforced in the LayoutGroupElement class)
		 * Each component or sub-group may specify size or alignment attributes as well as a group for the cross-orientation of its parent group
		 */
		boolean[] init = new boolean[1];
		parent.atts().require(this, LayoutAttributes.direction).takeUntil(until).act(v -> {
			if (init[0])
				parent.msg().error("Cannot modify the direction of a group layout dynamically");
		});
		until.act(v -> parent.atts().reject(this, LayoutAttributes.direction));
		Observable.or(new Observable[] { //
			parent.getStyle().get(LayoutStyle.margin), //
			parent.getStyle().get(LayoutStyle.padding)//
		}).takeUntil(until).act(evt -> //
		/**/parent.sizeNeedsChanged());
		// Make a tree composed of every sub-group and every component this layout will be directly concerned with laying out
		ObservableTree<QuickElement, QuickElement> groupTree = ObservableTree.of(//
			ObservableValue.constant(parent), //
			TypeToken.of(QuickElement.class), //
			el -> ObservableValue.constant(el), //
			el -> el instanceof LayoutGroupElement//
				? el.getPhysicalChildren()//
				: ObservableList.constant(TypeToken.of(QuickElement.class)));
		ObservableCollection<QuickElement> paths = ObservableTree.valuePathsOf(groupTree, false).takeUntil(until)
			.map(path -> path.get(path.size() - 1));
		// Listen for changes to the tree
		paths.simpleChanges().act(v -> //
		/**/parent.sizeNeedsChanged());
		// For each element in the tree, listen for its attributes
		paths.onElement(el -> {
			Observable<?> noInit = el.noInit();
			el.act(event -> {
				QuickElement element = event.getValue();
				if (element == parent)
					return;
				Observable<?>[] attObs = new Observable[CHILD_ATTRIBUTES.size() + 1];
				for (int i = 0; i < attObs.length; i++)
					attObs[i] = element.atts().accept(GroupLayout.this, CHILD_ATTRIBUTES.get(i));
				Direction dir = element.getParent().get().atts().get(LayoutAttributes.direction);
				if (element instanceof LayoutGroupElement
					&& element.atts().get(LayoutAttributes.direction).getOrientation() == dir.getOrientation())
					parent.msg().error("Child layout groups must have an orientation opposite of their parent");
				attObs[CHILD_ATTRIBUTES.size()] = element.atts().accept(GroupLayout.this, //
					dir.getOrientation() == Orientation.vertical ? H_GROUP : V_GROUP);
				Observable.or(attObs).takeUntil(noInit).act(evt ->{
					if(init[0])
						parent.sizeNeedsChanged();
				});
				noInit.act(v -> {
					for (QuickAttribute<?> att : CHILD_ATTRIBUTES)
						element.atts().reject(GroupLayout.this, att);
				});
			});
		});
		init[0] = true;
	}

	@Override
	public SizeGuide getWSizer(QuickElement parent, QuickElement[] children) {
		Map<String, NamedLayoutGroup> namedGroups = new LinkedHashMap<>();
		Direction dir = parent.atts().get(LayoutAttributes.direction);
		SequentialLayoutGroup outerGroup = groupFor(parent.getPhysicalChildren(), dir, namedGroups);
		return getSizer(outerGroup, dir.getOrientation(), !dir.getOrientation().isVertical());
	}

	@Override
	public SizeGuide getHSizer(QuickElement parent, QuickElement[] children) {
		Map<String, NamedLayoutGroup> namedGroups = new LinkedHashMap<>();
		Direction dir = parent.atts().get(LayoutAttributes.direction);
		SequentialLayoutGroup outerGroup = groupFor(parent.getPhysicalChildren(), dir, namedGroups);
		return getSizer(outerGroup, dir.getOrientation(), dir.getOrientation().isVertical());
	}

	private SizeGuide getSizer(SequentialLayoutGroup group, Orientation orient, boolean sequential) {
		return (sequential ? sequenceSizer(group, orient) : parallelSizer(group, orient)).asGuide();
	}

	private LayoutSpring sequenceSizer(LayoutGroup group, Orientation orient) {
		LayoutSpring[] components = new LayoutSpring[group.elements.size()];
		int i=0;
		for(Object el : group.elements){
			LayoutSpring component;
			if(el instanceof LayoutGroup)
				component= parallelSizer((LayoutGroup) el, orient.opposite());
			else
				component = new LayoutSpring.SimpleLayoutSpring(((QuickElement) el).bounds().get(orient).getGuide());
			components[i++]=component;
		}
		return new LayoutSpring.SeriesSpring(components);
	}

	private LayoutSpring parallelSizer(LayoutGroup group, Orientation orient) {
		LayoutSpring[] components = new LayoutSpring[group.elements.size()];
		int i = 0;
		for (Object el : group.elements) {
			LayoutSpring component;
			if (el instanceof LayoutGroup)
				component = sequenceSizer((LayoutGroup) el, orient.opposite());
			else
				component = new LayoutSpring.SimpleLayoutSpring(((QuickElement) el).bounds().get(orient).getGuide()); // TODO
			components[i++] = component;
		}
		return new LayoutSpring.ParallelSpring(components);
	}

	@Override
	public void layout(QuickElement parent, QuickElement[] children) {
		// TODO Auto-generated method stub

	}

	private static SequentialLayoutGroup groupFor(List<? extends QuickElement> children, Direction parentDir,
		Map<String, NamedLayoutGroup> namedGroups) {
		SequentialLayoutGroup outerGroup = new SequentialLayoutGroup(parentDir);
		for(QuickElement child : children){
			Object toAdd;
			if (child instanceof LayoutGroupElement)
				toAdd = groupFor(child.getPhysicalChildren(), child.atts().get(LayoutAttributes.direction), namedGroups);
			else
				toAdd = child;
			String groupName=child.atts().get(parentDir.getOrientation()==Orientation.vertical ? H_GROUP : V_GROUP);
			if (groupName != null)
				namedGroups.computeIfAbsent(groupName, gn -> new NamedLayoutGroup(gn)).elements.add(toAdd);
			outerGroup.elements.add(toAdd);
		}
		return outerGroup;
	}

	private static class LayoutGroup {
		final List<Object> elements;

		LayoutGroup() {
			elements = new ArrayList<>(5);
		}
	}

	private static class NamedLayoutGroup extends LayoutGroup {
		final String name;

		NamedLayoutGroup(String name) {
			this.name = name;
		}
	}

	private static class SequentialLayoutGroup extends LayoutGroup {
		final Direction direction;

		SequentialLayoutGroup(Direction direction) {
			this.direction = direction;
		}
	}
}
