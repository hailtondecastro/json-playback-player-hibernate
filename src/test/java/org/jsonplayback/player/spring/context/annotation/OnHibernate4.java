package org.jsonplayback.player.spring.context.annotation;

import org.jsonplayback.player.hibernate.HibernateVersion;
import org.jsonplayback.player.hibernate.PlayerManagerDefault;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Quando a property "WA_AMBIENTE" com o valor "DESENV_WORK_STATION".<br>
 * 
 * @author Hailton de Castro
 *
 */
public class OnHibernate4 implements Condition {
	public final static OnHibernate4 INSTANCE = new OnHibernate4();

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		return PlayerManagerDefault.getHibernateVersionStatic() == HibernateVersion.HB4;
	}
}
