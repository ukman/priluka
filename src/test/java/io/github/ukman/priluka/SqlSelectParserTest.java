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

    @Test
    void parsesFunctionCallInSelectList() {
        assertSql(
            "select count(*) from person",
            "select count(*) from person"
        );
    }

    @Test
    void parsesBetweenGroupByHavingAndOrderBy() {
        assertSql(
            "select last_name, count(*) from person group by last_name having count(*) between 1 and 10 order by last_name",
            "select last_name, count(*) from person group by last_name having count(*) between 1 and 10 order by last_name"
        );
    }

    @Test
    void parsesInCondition() {
        assertSql(
            "select first_name from person where id in (1, 2, 3)",
            "select first_name from person where id in (1, 2, 3)"
        );
    }

    @Test
    void parsesNotInConditionAndFunctionExpression() {
        assertSql(
            "select first_name from person where abs(person.id) not in (1, 2)",
            "select first_name from person where abs(person.id) not in (1, 2)"
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
            final GroupByClauseTail groupByClause;
            final HavingClauseTail havingClause;
            final OrderByClauseTail orderByClause;

            SelectStatement(
                Select select,
                SelectList selectList,
                From from,
                TableReference fromTable,
                WhereClauseTail whereClause,
                GroupByClauseTail groupByClause,
                HavingClauseTail havingClause,
                OrderByClauseTail orderByClause
            ) {
                this.select = select;
                this.selectList = selectList;
                this.from = from;
                this.fromTable = fromTable;
                this.whereClause = whereClause;
                this.groupByClause = groupByClause;
                this.havingClause = havingClause;
                this.orderByClause = orderByClause;
            }

            String sql() {
                return "select " + selectList.sql()
                    + " from " + fromTable.sql()
                    + whereClause.sql()
                    + groupByClause.sql()
                    + havingClause.sql()
                    + orderByClause.sql();
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

        static class FunctionSelectItem implements SelectItem {
            final FunctionCall functionCall;

            FunctionSelectItem(FunctionCall functionCall) {
                this.functionCall = functionCall;
            }

            @Override
            public String sql() {
                return functionCall.sql();
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

        interface GroupByClauseTail {
            String sql();
        }

        static class GroupByColumnsTail implements GroupByClauseTail {
            final Group group;
            final By by;
            final ValueExpressionList columns;

            GroupByColumnsTail(Group group, By by, ValueExpressionList columns) {
                this.group = group;
                this.by = by;
                this.columns = columns;
            }

            @Override
            public String sql() {
                return " group by " + columns.sql();
            }
        }

        static class ZEmptyGroupByClauseTail implements GroupByClauseTail {
            ZEmptyGroupByClauseTail() {
            }

            @Override
            public String sql() {
                return "";
            }
        }

        interface HavingClauseTail {
            String sql();
        }

        static class HavingConditionTail implements HavingClauseTail {
            final Having having;
            final ConditionalExpression condition;

            HavingConditionTail(Having having, ConditionalExpression condition) {
                this.having = having;
                this.condition = condition;
            }

            @Override
            public String sql() {
                return " having " + condition.sql();
            }
        }

        static class ZEmptyHavingClauseTail implements HavingClauseTail {
            ZEmptyHavingClauseTail() {
            }

            @Override
            public String sql() {
                return "";
            }
        }

        interface OrderByClauseTail {
            String sql();
        }

        static class OrderByColumnsTail implements OrderByClauseTail {
            final Order order;
            final By by;
            final ValueExpressionList columns;

            OrderByColumnsTail(Order order, By by, ValueExpressionList columns) {
                this.order = order;
                this.by = by;
                this.columns = columns;
            }

            @Override
            public String sql() {
                return " order by " + columns.sql();
            }
        }

        static class ZEmptyOrderByClauseTail implements OrderByClauseTail {
            ZEmptyOrderByClauseTail() {
            }

            @Override
            public String sql() {
                return "";
            }
        }

        interface ConditionalExpression {
            String sql();
        }

        static class EqualsConditionalExpression implements ConditionalExpression {
            final ValueExpression left;
            final Equals equals;
            final ValueExpression right;

            EqualsConditionalExpression(ValueExpression left, Equals equals, ValueExpression right) {
                this.left = left;
                this.equals = equals;
                this.right = right;
            }

            @Override
            public String sql() {
                return left.sql() + " = " + right.sql();
            }
        }

        static class BetweenConditionalExpression implements ConditionalExpression {
            final ValueExpression value;
            final Between between;
            final ValueExpression lower;
            final And and;
            final ValueExpression upper;

            BetweenConditionalExpression(ValueExpression value, Between between, ValueExpression lower, And and, ValueExpression upper) {
                this.value = value;
                this.between = between;
                this.lower = lower;
                this.and = and;
                this.upper = upper;
            }

            @Override
            public String sql() {
                return value.sql() + " between " + lower.sql() + " and " + upper.sql();
            }
        }

        static class InConditionalExpression implements ConditionalExpression {
            final ValueExpression value;
            final In in;
            final OpenParen openParen;
            final ValueExpressionList values;
            final CloseParen closeParen;

            InConditionalExpression(ValueExpression value, In in, OpenParen openParen, ValueExpressionList values, CloseParen closeParen) {
                this.value = value;
                this.in = in;
                this.openParen = openParen;
                this.values = values;
                this.closeParen = closeParen;
            }

            @Override
            public String sql() {
                return value.sql() + " in (" + values.sql() + ")";
            }
        }

        static class NotInConditionalExpression implements ConditionalExpression {
            final ValueExpression value;
            final Not not;
            final In in;
            final OpenParen openParen;
            final ValueExpressionList values;
            final CloseParen closeParen;

            NotInConditionalExpression(
                ValueExpression value,
                Not not,
                In in,
                OpenParen openParen,
                ValueExpressionList values,
                CloseParen closeParen
            ) {
                this.value = value;
                this.not = not;
                this.in = in;
                this.openParen = openParen;
                this.values = values;
                this.closeParen = closeParen;
            }

            @Override
            public String sql() {
                return value.sql() + " not in (" + values.sql() + ")";
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

        static class FunctionValueExpression implements ValueExpression {
            final FunctionCall functionCall;

            FunctionValueExpression(FunctionCall functionCall) {
                this.functionCall = functionCall;
            }

            @Override
            public String sql() {
                return functionCall.sql();
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

        static class FunctionCall {
            final QualifiedName name;
            final OpenParen openParen;
            final FunctionArguments arguments;
            final CloseParen closeParen;

            FunctionCall(QualifiedName name, OpenParen openParen, FunctionArguments arguments, CloseParen closeParen) {
                this.name = name;
                this.openParen = openParen;
                this.arguments = arguments;
                this.closeParen = closeParen;
            }

            String sql() {
                return name.sql() + "(" + arguments.sql() + ")";
            }
        }

        interface FunctionArguments {
            String sql();
        }

        static class StarFunctionArguments implements FunctionArguments {
            final Star star;

            StarFunctionArguments(Star star) {
                this.star = star;
            }

            @Override
            public String sql() {
                return "*";
            }
        }

        static class ValueListFunctionArguments implements FunctionArguments {
            final ValueExpressionList values;

            ValueListFunctionArguments(ValueExpressionList values) {
                this.values = values;
            }

            @Override
            public String sql() {
                return values.sql();
            }
        }

        static class ValueExpressionList {
            final ValueExpression value;
            final ValueExpressionListTail tail;

            ValueExpressionList(ValueExpression value, ValueExpressionListTail tail) {
                this.value = value;
                this.tail = tail;
            }

            String sql() {
                return value.sql() + tail.sql();
            }
        }

        interface ValueExpressionListTail {
            String sql();
        }

        static class CommaValueExpressionListTail implements ValueExpressionListTail {
            final Comma comma;
            final ValueExpression value;
            final ValueExpressionListTail tail;

            CommaValueExpressionListTail(Comma comma, ValueExpression value, ValueExpressionListTail tail) {
                this.comma = comma;
                this.value = value;
                this.tail = tail;
            }

            @Override
            public String sql() {
                return ", " + value.sql() + tail.sql();
            }
        }

        static class ZEmptyValueExpressionListTail implements ValueExpressionListTail {
            ZEmptyValueExpressionListTail() {
            }

            @Override
            public String sql() {
                return "";
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

        @Keyword("group")
        static class Group {
        }

        @Keyword("by")
        static class By {
        }

        @Keyword("having")
        static class Having {
        }

        @Keyword("order")
        static class Order {
        }

        @Keyword("between")
        static class Between {
        }

        @Keyword("and")
        static class And {
        }

        @Keyword("in")
        static class In {
        }

        @Keyword("not")
        static class Not {
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
