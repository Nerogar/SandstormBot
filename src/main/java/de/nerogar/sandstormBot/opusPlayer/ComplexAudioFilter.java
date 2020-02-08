package de.nerogar.sandstormBot.opusPlayer;

import java.util.ArrayList;
import java.util.List;

public class ComplexAudioFilter {

	private List<Node> nodes;

	public ComplexAudioFilter() {
		nodes = new ArrayList<>();
	}

	public void addFilter(String filter) {
		nodes.add(new Node(filter));
	}

	public String buildFilterString() {
		StringBuilder sb = new StringBuilder();

		int currentFilter = 0;
		for (; currentFilter < nodes.size(); currentFilter++) {
			if (currentFilter == 0) {
				sb.append("[0:a]");
			} else {
				sb.append("[filter").append(currentFilter - 1).append(']');
			}

			sb.append(nodes.get(currentFilter).filter);

			sb.append("[filter").append(currentFilter).append(']');
		}

		return sb.toString();
	}

	public String getFilterOutputString() {
		if (nodes.size() > 0) {
			return "[filter" + (nodes.size() - 1) + "]";
		} else {
			return "[0:a]";
		}
	}

	private static class Node {

		public String filter;

		public Node(String filter) {
			this.filter = filter;
		}
	}

}
