package com.lunex.eventprocessor.core.dataaccess;

import java.util.Collection;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;

public class StatementBucket {

  private BatchStatement batchStatement;

  public BatchStatement getBatchStatement() {
    return batchStatement;
  }

  public void setBatchStatement(BatchStatement batchStatement) {
    this.batchStatement = batchStatement;
  }

  private Session session;

  public Session getSession() {
    return session;
  }

  public void setSession(Session session) {
    this.session = session;
  }

  public StatementBucket(Session session) {
    this.session = session;
  }

  public Collection<Statement> getStatementList() {
    return this.batchStatement.getStatements();
  }

  public void add(Statement statement) {
    this.batchStatement.add(statement);
  }

  public void addAll(Iterable<Statement> statements) {
    this.batchStatement.addAll(statements);
  }

  public void clear() {
    this.batchStatement.clear();
  }

  /**
   * Statements will be cleared after executing batch
   */
  public void execute() {
    this.execute(true);
  }

  public void execute(boolean cleanUpStatement) {
    if (this.batchStatement.getStatements().size() > 0) {
      this.session.execute(batchStatement);
      if (cleanUpStatement == true) {
        this.batchStatement.clear();
      }
    }
  }
}
