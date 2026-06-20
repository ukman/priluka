package io.github.ukman.priluka;

import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.Terminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlSelectParserTest {
    @Test
    void parsesSelectAllFromTable() {
        assertSql("select * from person", "select * from person");
    }

    @Test
    void parsesQualifiedStarAndBareAlias() {
        assertSql("select p.* from person p", "select p.* from person p");
    }

    @Test
    void parsesSubqueryInFrom() {
        assertSql(
            "select * from (select * from person) as t",
            "select * from (select * from person) as t"
        );
    }

    @Test
    void parsesSelectListLeftJoinAndWhereCondition() {
        assertSql(
            "select last_name, first_name from db.person as p left join db.company c where p.company_id = c.id",
            "select last_name, first_name from db.person as p left join db.company c where p.company_id = c.id"
        );
    }

    private void assertSql(String input, String expected) {
        SqlGrammar.SelectStatement statement = Parser
            .initFromOuterClass(SqlGrammar.class)
            .parse(SqlGrammar.SelectStatement.class, input);

        assertEquals(expected, statement.sql());
    }

    static final class SqlGrammar {
        static class SelectStatement {
            final Select select;
            final SelectList selectList;
            final From from;
            final TableReference fromTable;
            final WhereClauseTail whereClause;

            SelectStatement(Select select, SelectList selectList, From from, TableReference fromTable, WhereClauseTail whereClause) {
                this.select = select;
                this.selectList = selectList;
                this.from = from;
                this.fromTable = fromTable;
                this.whereClause = whereClause;
            }

            String sql() {
                return "select " + selectList.sql() + " from " + fromTable.sql() + whereClause.sql();
            }
        }

        static class SelectList {
            final SelectItem item;
            final SelectListTail tail;

            SelectList(SelectItem item, SelectListTail tail) {
                this.item = item;
                this.tail = tail;
            }

            String sql() {
                return item.sql() + tail.sql();
            }
        }

        interface SelectListTail {
            String sql();
        }

        static class CommaSelectListTail implements SelectListTail {
            final Comma comma;
            final SelectItem item;
            final SelectListTail tail;

            CommaSelectListTail(Comma comma, SelectItem item, SelectListTail tail) {
                this.comma = comma;
                this.item = item;
                this.tail = tail;
            }

            @Override
            public String sql() {
                return ", " + item.sql() + tail.sql();
            }
        }

        static class ZEmptySelectListTail implements SelectListTail {
            ZEmptySelectListTail() {
            }

            @Override
            public String sql() {
                return "";
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

        static class QualifiedAllColumns implements SelectItem {
            final QualifiedName qualifier;
            final Dot dot;
            final Star star;

            QualifiedAllColumns(QualifiedName qualifier, Dot dot, Star star) {
                this.qualifier = qualifier;
                this.dot = dot;
                this.star = star;
            }

            @Override
            public String sql() {
                return qualifier.sql() + ".*";
            }
        }

        static class TableReference {
            final TablePrimary primary;
            final AliasTail alias;
            final JoinTail joins;

            TableReference(TablePrimary primary, AliasTail alias, JoinTail joins) {
                this.primary = primary;
                this.alias = alias;
                this.joins = joins;
            }

            String sql() {
                return primary.sql() + alias.sql() + joins.sql();
            }
        }

        interface TablePrimary {
            String sql();
        }

        static class NamedTable implements TablePrimary {
            final QualifiedName name;

            NamedTable(QualifiedName name) {
                this.name = name;
            }

            @Override
            public String sql() {
                return name.sql();
            }
        }

        static class SubqueryTable implements TablePrimary {
            final OpenParen openParen;
            final SelectStatement select;
            final CloseParen closeParen;

            SubqueryTable(OpenParen openParen, SelectStatement select, CloseParen closeParen) {
                this.openParen = openParen;
                this.select = select;
                this.closeParen = closeParen;
            }

            @Override
            public String sql() {
                return "(" + select.sql() + ")";
            }
        }

        interface AliasTail {
            String sql();
        }

        static class AsAliasTail implements AliasTail {
            final As as;
            final Id alias;

            AsAliasTail(As as, Id alias) {
                this.as = as;
                this.alias = alias;
            }

            @Override
            public String sql() {
                return " as " + alias.sql();
            }
        }

        static class BareAliasTail implements AliasTail {
            final Id alias;

            BareAliasTail(Id alias) {
                this.alias = alias;
            }

            @Override
            public String sql() {
                return " " + alias.sql();
            }
        }

        static class ZEmptyAliasTail implements AliasTail {
            ZEmptyAliasTail() {
            }

            @Override
            public String sql() {
                return "";
            }
        }

        interface JoinTail {
            String sql();
        }

        static class LeftJoinTail implements JoinTail {
            final Left left;
            final Join join;
            final TableReference table;

            LeftJoinTail(Left left, Join join, TableReference table) {
                this.left = left;
                this.join = join;
                this.table = table;
            }

            @Override
            public String sql() {
                return " left join " + table.sql();
            }
        }

        static class ZEmptyJoinTail implements JoinTail {
            ZEmptyJoinTail() {
            }

            @Override
            public String sql() {
                return "";
            }
        }

        interface WhereClauseTail {
            String sql();
        }

        static class WhereConditionTail implements WhereClauseTail {
            final Where where;
            final ConditionalExpression condition;

            WhereConditionTail(Where where, ConditionalExpression condition) {
                this.where = where;
                this.condition = condition;
            }

            @Override
            public String sql() {
                return " where " + condition.sql();
            }
        }

        static class ZEmptyWhereClauseTail implements WhereClauseTail {
            ZEmptyWhereClauseTail() {
            }

            @Override
            public String sql() {
                return "";
            }
        }

        static class ConditionalExpression {
            final ValueExpression left;
            final Equals equals;
            final ValueExpression right;

            ConditionalExpression(ValueExpression left, Equals equals, ValueExpression right) {
                this.left = left;
                this.equals = equals;
                this.right = right;
            }

            String sql() {
                return left.sql() + " = " + right.sql();
            }
        }

        interface ValueExpression {
            String sql();
        }

        static class NamedValueExpression implements ValueExpression {
            final QualifiedName name;

            NamedValueExpression(QualifiedName name) {
                this.name = name;
            }

            @Override
            public String sql() {
                return name.sql();
            }
        }

        static class NumberValueExpression implements ValueExpression {
            final Integer value;

            NumberValueExpression(Integer value) {
                this.value = value;
            }

            @Override
            public String sql() {
                return value.toString();
            }
        }

        static class ParenthesizedValueExpression implements ValueExpression {
            final OpenParen openParen;
            final ValueExpression value;
            final CloseParen closeParen;

            ParenthesizedValueExpression(OpenParen openParen, ValueExpression value, CloseParen closeParen) {
                this.openParen = openParen;
                this.value = value;
                this.closeParen = closeParen;
            }

            @Override
            public String sql() {
                return "(" + value.sql() + ")";
            }
        }

        static class QualifiedName {
            final Id head;
            final QualifiedNameTail tail;

            QualifiedName(Id head, QualifiedNameTail tail) {
                this.head = head;
                this.tail = tail;
            }

            String sql() {
                return head.sql() + tail.sql();
            }
        }

        interface QualifiedNameTail {
            String sql();
        }

        static class DotQualifiedNameTail implements QualifiedNameTail {
            final Dot dot;
            final Id name;
            final QualifiedNameTail tail;

            DotQualifiedNameTail(Dot dot, Id name, QualifiedNameTail tail) {
                this.dot = dot;
                this.name = name;
                this.tail = tail;
            }

            @Override
            public String sql() {
                return "." + name.sql() + tail.sql();
            }
        }

        static class ZEmptyQualifiedNameTail implements QualifiedNameTail {
            ZEmptyQualifiedNameTail() {
            }

            @Override
            public String sql() {
                return "";
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

        @Keyword("as")
        static class As {
        }

        @Keyword("left")
        static class Left {
        }

        @Keyword("join")
        static class Join {
        }

        @Keyword("where")
        static class Where {
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

        @Keyword("(")
        static class OpenParen {
        }

        @Keyword(")")
        static class CloseParen {
        }
    }
}
