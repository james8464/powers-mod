package com.powers.knowledge;

/** Pluggable Shadow answer source; offline registry/datapack providers are default. */
public interface KnowledgeProvider {
	KnowledgeAnswer answer(KnowledgeQuery query);
}
