package cz.znj.kvr.sw.exp.java.corejava.stream;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * @author
 * 	Zbynek Vyskovsky
 */
public class GroupPartitionTest
{
	@Test
	public void testGrouping()
	{
		List<String> items = ImmutableList.<String>builder()
				.add("Zbynek")
				.add("Vyskovsky")
				.add("Lumir")
				.add("Zdenek")
				.build();
		// no way to do it in Java 8 Streams.
//		items.stream().reduce(
//				Collections.emptyList(),
//				(List<String> left, String right) -> {
//					if (left.isEmpty()) {
//						List<String> l = new ArrayList<>();
//						l.add(right);
//						return l;
//					}
//					else if (left.get(0).isEmpty() ? right.isEmpty() : right.isEmpty() ? false : left.get(0).charAt(0) == right.charAt(0)) {
//						left.add(right);
//						return left;
//					}
//					else {
//						List<String> l = new ArrayList<>();
//						l.add(right);
//						return l;
//					}
//				}
//		);
	}
}
