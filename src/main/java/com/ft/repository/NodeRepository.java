package com.ft.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.stereotype.Repository;

import com.ft.domain.Node;
import com.ft.domain.QNode;

/**
 * Spring Data MongoDB repository for the {@link Node} entity.
 */
@Repository
public interface NodeRepository extends ReactiveMongoRepository<Node, String>, ReactiveQuerydslPredicateExecutor<Node>, QuerydslBinderCustomizer<QNode> {
	
	@Override
	default void customize(QuerydslBindings bindings, QNode root) {
		// TODO Auto-generated method stub
		
	}

}
