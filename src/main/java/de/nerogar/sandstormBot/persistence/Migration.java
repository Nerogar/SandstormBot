package de.nerogar.sandstormBot.persistence;

import java.util.ArrayList;
import java.util.List;

public class Migration {

	private String       name;
	private List<String> statements;

	public Migration(String name, String migration) {
		this.name = name;
		this.statements = splitStatements(migration);
	}

	private List<String> splitStatements(String migration) {
		int[] lastCodePoint = { 0 };

		boolean[] inBlockComment = { false };
		boolean[] inLineComment = { false };
		boolean[] inString = { false };
		boolean[] inIdentifier = { false };
		boolean[] isEscaped = { false };

		List<String> statements = new ArrayList<>();
		StringBuilder currentStatement = new StringBuilder();

		migration.codePoints().forEach(codepoint -> {
			if (inBlockComment[0]) {
				if (lastCodePoint[0] == '*' && codepoint == '/') {
					inBlockComment[0] = false;
				}
			} else if (inLineComment[0]) {
				if (codepoint == '\n') {
					inLineComment[0] = false;
				}
			} else if (inString[0]) {
				if (isEscaped[0]) {
					isEscaped[0] = false;
				} else {
					if (codepoint == '\\') {
						isEscaped[0] = true;
					} else if (codepoint == '\'') {
						inString[0] = false;
					}
				}
			} else if (inIdentifier[0]) {
				if (isEscaped[0]) {
					isEscaped[0] = false;
				} else {
					if (codepoint == '\\') {
						isEscaped[0] = true;
					} else if (codepoint == '\"') {
						inIdentifier[0] = false;
					}
				}
			} else {
				if (codepoint == '*' && lastCodePoint[0] == '/') {
					inBlockComment[0] = true;
				} else if (codepoint == '-' && lastCodePoint[0] == '-') {
					inLineComment[0] = true;
				} else if (codepoint == '\'') {
					inString[0] = true;
				} else if (codepoint == '\"') {
					inIdentifier[0] = true;
				} else if (codepoint == ';') {
					statements.add(currentStatement.toString());
					currentStatement.setLength(0);
					return;
				}
			}

			currentStatement.appendCodePoint(codepoint);
			lastCodePoint[0] = codepoint;
		});

		statements.add(currentStatement.toString());
		statements.removeIf(String::isBlank);

		return statements;
	}

	public String getName()             { return name; }

	public List<String> getStatements() { return statements; }

}
