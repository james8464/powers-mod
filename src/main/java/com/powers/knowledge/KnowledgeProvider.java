package com.powers.knowledge;

/** Pluggable Knowledge Book answer source; offline registry/datapack providers are default. */
public interface KnowledgeProvider {
	KnowledgeAnswer answer(KnowledgeQuery query);
}
