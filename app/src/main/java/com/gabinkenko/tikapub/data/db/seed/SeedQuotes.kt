package com.gabinkenko.tikapub.data.db.seed

import com.gabinkenko.tikapub.data.db.QuoteEntity

/**
 * Starter content so the app can publish from day one. All entries are original one-liners or
 * traditional/public-domain proverbs (no attribution to living authors) to avoid copyright issues -
 * replace or extend these from the Quotes screen with your own material.
 */
object SeedQuotes {

    fun all(): List<QuoteEntity> = buildList {
        motivation.forEach { add(QuoteEntity(text = it, category = "motivation")) }
        sagesse.forEach { add(QuoteEntity(text = it, category = "sagesse")) }
        humour.forEach { add(QuoteEntity(text = it, category = "humour")) }
        business.forEach { add(QuoteEntity(text = it, category = "business")) }
    }

    private val motivation = listOf(
        "Chaque jour est une nouvelle chance de recommencer.",
        "Le succès, c'est avancer d'échec en échec sans perdre son enthousiasme.",
        "Ce n'est pas la charge qui casse, c'est la façon de la porter.",
        "Les grandes réussites commencent toujours par une petite décision.",
        "On ne voit bien qu'avec le cœur, l'essentiel est invisible pour les yeux.",
        "L'action est la clé fondamentale de toute réussite.",
        "Ne compte pas les jours, fais que les jours comptent.",
        "Le meilleur moment pour commencer, c'était hier. Le deuxième meilleur, c'est maintenant.",
        "Un obstacle est souvent un tremplin déguisé.",
        "Fais de ta vie un rêve, et d'un rêve, une réalité.",
    )

    private val sagesse = listOf(
        "Qui veut voyager loin ménage sa monture.",
        "Petit à petit, l'oiseau fait son nid.",
        "Il n'y a pas de vent favorable pour celui qui ne sait où il va.",
        "La patience est amère, mais son fruit est doux.",
        "On ne change pas une équipe qui gagne, mais on ne progresse pas sans changer.",
        "Le temps qu'on aime passer perdu n'est jamais vraiment perdu.",
        "Mieux vaut allumer une bougie que maudire l'obscurité.",
        "La vraie sagesse est de savoir ce que l'on ne sait pas.",
        "Ce que l'on sème dans la discrétion, on le récolte dans la lumière.",
        "Un voyage de mille lieues commence toujours par un premier pas.",
    )

    private val humour = listOf(
        "J'ai un plan pour être productif aujourd'hui. On verra demain s'il fonctionne.",
        "Le café : parce qu'un jour sans café, c'est un peu comme... je ne sais pas, je n'ai jamais essayé.",
        "Je ne procrastine pas, je laisse mûrir mes idées. Longtemps.",
        "Mon lit et moi, c'est une histoire d'amour que le réveil interrompt chaque matin.",
        "Non, je ne suis pas en retard. Tout le monde est juste en avance.",
        "La motivation, c'est comme le wifi du voisin : ça marche un jour sur deux.",
        "Je fais du sport : je cours après le temps toute la journée.",
        "Un jour sans rire est un jour où j'ai oublié de checker mes notifications.",
    )

    private val business = listOf(
        "Une idée ne vaut rien tant qu'elle n'est pas exécutée.",
        "Le client n'a pas toujours raison, mais il a toujours quelque chose à t'apprendre.",
        "La discipline bat la motivation à chaque fois.",
        "Ton réseau est ton premier capital.",
        "Livre vite, apprends vite, ajuste vite.",
        "La régularité bat l'intensité sur le long terme.",
        "Un bon produit se vend, un excellent produit se recommande.",
        "Investir en toi-même rapporte toujours des intérêts.",
    )
}
