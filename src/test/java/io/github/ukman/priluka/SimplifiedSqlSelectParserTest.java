package io.github.ukman.priluka;

import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.OneOrMore;
import io.github.ukman.priluka.annotation.Separator;
import io.github.ukman.priluka.annotation.Terminal;
import io.github.ukman.priluka.grammar.GrammarModel;
import io.github.ukman.priluka.internal.nfa.NfaFindResult;
import io.github.ukman.priluka.internal.nfa.NfaRecognizer;
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

    @Test
    void parsesMultipleLeftJoins() {
        assertSql(
            "select t1.name, t2.firstname from table1 t1 left join table2 t2 on t1.id=t2.id left join table3 t3 on t2.id=t3.id",
            "select t1.name, t2.firstname from table1 t1 left join table2 t2 on t1.id = t2.id left join table3 t3 on t2.id = t3.id"
        );
    }

    @Test
    void parsesDifferentJoinSpecs() {
        assertSql(
            "select t1.name from table1 t1 join table2 t2 on t1.id=t2.id right join table3 t3 on t2.id=t3.id outer join table4 t4 on t3.id=t4.id inner join table5 t5 on t4.id=t5.id",
            "select t1.name from table1 t1 join table2 t2 on t1.id = t2.id right join table3 t3 on t2.id = t3.id outer join table4 t4 on t3.id = t4.id inner join table5 t5 on t4.id = t5.id"
        );
    }

    @Test
    void nfaFindLocatesSqlInsideTokenStream() {
        String prefix = "noise words before query ";
        String sql = "select t1.name from table1 t1 join table2 t2 on t1.id=t2.id";
        GrammarModel model = Parser
            .initFromOuterClass(SimpleSqlGrammar.class)
            .describe(SimpleSqlGrammar.SelectStatement.class);

        NfaFindResult result = new NfaRecognizer(model).find(prefix + sql);
        SimpleSqlGrammar.SelectStatement statement = Parser.buildFromTrace(
            SimpleSqlGrammar.SelectStatement.class,
            result.getTrace()
        );

        assertEquals(prefix.length(), result.getStart());
        assertEquals(prefix.length() + sql.length(), result.getEnd());
        assertEquals("select t1.name from table1 t1 join table2 t2 on t1.id = t2.id", statement.sql());
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
            final FromClause fromClause;

            SelectStatement(
                Select select,
                @OneOrMore @Separator(Comma.class) SelectItem[] selectItems,
                From from,
                FromClause fromClause
            ) {
                this.select = select;
                this.selectItems = selectItems;
                this.from = from;
                this.fromClause = fromClause;
            }

            String sql() {
                return "select " + join(selectItems) + " from " + fromClause.sql();
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

        static class FromClause {
            final TableDef tableDef;
            final JoinClause[] joins;

            FromClause(TableDef tableDef, JoinClause[] joins) {
                this.tableDef = tableDef;
                this.joins = joins;
            }

            String sql() {
                StringBuilder result = new StringBuilder(tableDef.sql());
                for (int i = 0; i < joins.length; i++) {
                    result.append(joins[i].sql());
                }
                return result.toString();
            }
        }

        static class TableDef {
            final QualifiedName name;
            final Optional<Id> alias;

            TableDef(QualifiedName name, Optional<Id> alias) {
                this.name = name;
                this.alias = alias;
            }

            String sql() {
                String result = name.sql();
                if (alias.isPresent()) {
                    result += " " + alias.get().sql();
                }
                return result;
            }
        }

        static class JoinClause {
            final JoinSpec joinSpec;
            final TableDef tableDef;
            final On on;
            final QualifiedName leftName;
            final Equals equals;
            final QualifiedName rightName;

            JoinClause(
                JoinSpec joinSpec,
                TableDef tableDef,
                On on,
                QualifiedName leftName,
                Equals equals,
                QualifiedName rightName
            ) {
                this.joinSpec = joinSpec;
                this.tableDef = tableDef;
                this.on = on;
                this.leftName = leftName;
                this.equals = equals;
                this.rightName = rightName;
            }

            String sql() {
                return " " + joinSpec.sql() + " " + tableDef.sql()
                    + " on " + leftName.sql()
                    + " = " + rightName.sql();
            }
        }

        static class JoinSpec {
            final Optional<JoinModifier> modifier;
            final Join join;

            JoinSpec(Optional<JoinModifier> modifier, Join join) {
                this.modifier = modifier;
                this.join = join;
            }

            String sql() {
                if (modifier.isPresent()) {
                    return modifier.get().sql() + " join";
                }
                return "join";
            }
        }

        interface JoinModifier {
            String sql();
        }

        static class LeftJoinModifier implements JoinModifier {
            final Left left;

            LeftJoinModifier(Left left) {
                this.left = left;
            }

            @Override
            public String sql() {
                return "left";
            }
        }

        static class RightJoinModifier implements JoinModifier {
            final Right right;

            RightJoinModifier(Right right) {
                this.right = right;
            }

            @Override
            public String sql() {
                return "right";
            }
        }

        static class OuterJoinModifier implements JoinModifier {
            final Outer outer;

            OuterJoinModifier(Outer outer) {
                this.outer = outer;
            }

            @Override
            public String sql() {
                return "outer";
            }
        }

        static class InnerJoinModifier implements JoinModifier {
            final Inner inner;

            InnerJoinModifier(Inner inner) {
                this.inner = inner;
            }

            @Override
            public String sql() {
                return "inner";
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

        @Keyword("right")
        static class Right {
        }

        @Keyword("outer")
        static class Outer {
        }

        @Keyword("inner")
        static class Inner {
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
