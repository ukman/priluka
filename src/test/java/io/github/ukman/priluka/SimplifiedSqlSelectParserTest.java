package io.github.ukman.priluka;

import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.OneOrMore;
import io.github.ukman.priluka.annotation.Separator;
import io.github.ukman.priluka.annotation.Terminal;
import io.github.ukman.priluka.grammar.GrammarModel;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimplifiedSqlSelectParserTest {
    @Test
    void simplifiedSqlGrammarIsNfaCompatible() {
        GrammarModel model = Parser
            .initFromOuterClass(SimpleSqlGrammar.class)
            .describe(SimpleSqlGrammar.SelectStatement.class);

        assertTrue(model.checkNfaCompatibility().isSupported());
    }

    @Test
    void parsesSelectAll() {
        assertSql("select * from person", "select * from person");
    }

    @Test
    void parsesQualifiedAll() {
        assertSql("select t.* from person t", "select t.* from person t");
    }

    @Test
    void parsesFieldList() {
        assertSql(
            "select t.name, t.firstname, p.* from db.person p",
            "select t.name, t.firstname, p.* from db.person p"
        );
    }

    @Test
    void parsesSingleLeftJoin() {
        assertSql(
            "select t1.name, t2.firstname from table1 t1 left join table2 t2 on t1.id=t2.id",
            "select t1.name, t2.firstname from table1 t1 left join table2 t2 on t1.id = t2.id"
        );
    }

    private void assertSql(String input, String expected) {
        SimpleSqlGrammar.SelectStatement statement = Parser
            .initFromOuterClass(SimpleSqlGrammar.class)
            .parse(SimpleSqlGrammar.SelectStatement.class, input);

        assertEquals(expected, statement.sql());
    }

    static final class SimpleSqlGrammar {
        static class SelectStatement {
            final Select select;
            final SelectItem[] selectItems;
            final From from;
            final TableReference table;

            SelectStatement(
                Select select,
                @OneOrMore @Separator(Comma.class) SelectItem[] selectItems,
                From from,
                TableReference table
            ) {
                this.select = select;
                this.selectItems = selectItems;
                this.from = from;
                this.table = table;
            }

            String sql() {
                return "select " + join(selectItems) + " from " + table.sql();
            }
        }

        interface SelectItem {
            String sql();
        }

        static class AllColumns implements SelectItem {
            final Star star;

            AllColumns(Star star) {
                this.star = star;
            }

            @Override
            public String sql() {
                return "*";
            }
        }

        static class QualifiedAllColumns implements SelectItem {
            final Id qualifier;
            final Dot dot;
            final Star star;

            QualifiedAllColumns(Id qualifier, Dot dot, Star star) {
                this.qualifier = qualifier;
                this.dot = dot;
                this.star = star;
            }

            @Override
            public String sql() {
                return qualifier.sql() + ".*";
            }
        }

        static class NamedSelectItem implements SelectItem {
            final QualifiedName name;

            NamedSelectItem(QualifiedName name) {
                this.name = name;
            }

            @Override
            public String sql() {
                return name.sql();
            }
        }

        static class TableReference {
            final QualifiedName name;
            final Optional<Id> alias;
            final Optional<LeftJoinClause> join;

            TableReference(QualifiedName name, Optional<Id> alias, Optional<LeftJoinClause> join) {
                this.name = name;
                this.alias = alias;
                this.join = join;
            }

            String sql() {
                String result = name.sql();
                if (alias.isPresent()) {
                    result += " " + alias.get().sql();
                }
                if (join.isPresent()) {
                    result += join.get().sql();
                }
                return result;
            }
        }

        static class LeftJoinClause {
            final Left left;
            final Join join;
            final QualifiedName table;
            final Id alias;
            final On on;
            final QualifiedName leftName;
            final Equals equals;
            final QualifiedName rightName;

            LeftJoinClause(
                Left left,
                Join join,
                QualifiedName table,
                Id alias,
                On on,
                QualifiedName leftName,
                Equals equals,
                QualifiedName rightName
            ) {
                this.left = left;
                this.join = join;
                this.table = table;
                this.alias = alias;
                this.on = on;
                this.leftName = leftName;
                this.equals = equals;
                this.rightName = rightName;
            }

            String sql() {
                return " left join " + table.sql()
                    + " " + alias.sql()
                    + " on " + leftName.sql()
                    + " = " + rightName.sql();
            }
        }

        static class QualifiedName {
            final Id first;
            final Dot dot;
            final Id second;

            QualifiedName(Id first) {
                this(first, null, null);
            }

            QualifiedName(Id first, Dot dot, Id second) {
                this.first = first;
                this.dot = dot;
                this.second = second;
            }

            String sql() {
                if (second == null) {
                    return first.sql();
                }
                return first.sql() + "." + second.sql();
            }
        }

        @Terminal(regexp = "[A-Za-z_][A-Za-z0-9_]*")
        static class Id {
            final String text;

            Id(String text) {
                this.text = text;
            }

            String sql() {
                return text;
            }
        }

        @Keyword("select")
        static class Select {
        }

        @Keyword("from")
        static class From {
        }

        @Keyword("left")
        static class Left {
        }

        @Keyword("join")
        static class Join {
        }

        @Keyword("on")
        static class On {
        }

        @Keyword("*")
        static class Star {
        }

        @Keyword(".")
        static class Dot {
        }

        @Keyword(",")
        static class Comma {
        }

        @Keyword("=")
        static class Equals {
        }

        private static String join(SelectItem[] items) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < items.length; i++) {
                if (i > 0) {
                    result.append(", ");
                }
                result.append(items[i].sql());
            }
            return result.toString();
        }
    }
}
