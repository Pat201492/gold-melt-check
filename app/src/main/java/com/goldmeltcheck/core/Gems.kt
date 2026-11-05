package com.goldmeltcheck.core

/** What you need to see a sign. Ordered from what anyone can do to what needs a lab. */
enum class Check(val label: String) {
    EYE("By eye"),
    LOUPE("10× loupe"),
    UV("UV light"),
    TOOL("Tool"),
    LAB("Lab only"),
}

data class GemSign(val check: Check, val text: String)

data class Gem(
    val id: String,
    val name: String,
    val summary: String,
    val signs: List<GemSign>,
    /** Typical retail USD per carat for about a 1 ct stone; null where no reliable figure was found. */
    val natural: PriceRange?,
    val lab: PriceRange?,
)

/**
 * Field guide to lab-grown and imitation stones, plus price ballparks.
 * Signs are hints, not proof: many lab stones show none of them, and only a gem lab can be sure.
 */
object Gems {
    /** When the price ballparks were compiled. Update all three together whenever the ranges change. */
    const val PRICES_YEAR = 2026
    const val PRICES_MONTH = 10
    const val PRICES_AS_OF = "Oct 2026"

    /** Ballparks older than this many months get a warning in the app. */
    const val STALE_AFTER_MONTHS = 6

    /** Whole months between the ballpark date and [year]/[month] (1-12). */
    fun ageInMonths(year: Int, month: Int): Int = (year - PRICES_YEAR) * 12 + (month - PRICES_MONTH)

    fun isStale(year: Int, month: Int): Boolean = ageInMonths(year, month) >= STALE_AFTER_MONTHS

    val general = listOf(
        GemSign(Check.EYE, "A large, flawless, vividly colored stone at a bargain price is the most common warning sign."),
        GemSign(Check.EYE, "Ask for the lab report and read it. \"Laboratory-grown\", \"synthetic\" or \"created\" on it settles the question."),
        GemSign(Check.LOUPE, "Look for a laser inscription on the girdle (the thin outer edge). Lab-grown diamonds are often inscribed \"LG\" or \"Lab Grown\" with a report number."),
        GemSign(Check.LAB, "Lab-grown stones are the same material as natural ones. Standard testers pass them. For anything expensive, only a gem lab (GIA, IGI, AGL, Gübelin, SSEF) can confirm natural origin."),
    )

    val all = listOf(
        Gem(
            id = "diamond",
            name = "Diamond",
            summary = "Lab diamonds are real diamond, so a standard diamond tester passes them. The clues are inclusions, glow under UV, and the paperwork.",
            signs = listOf(
                GemSign(Check.EYE, "Near-colorless and nearly flawless at well under natural prices: lab diamonds sell for a small fraction of natural ones."),
                GemSign(Check.LOUPE, "Shiny dark metallic specks or rods: flux metal left by HPHT growth. Natural diamonds don't contain metal."),
                GemSign(Check.LOUPE, "Dark pinpoint specks of graphite, or faint parallel banding: common in CVD-grown diamonds."),
                GemSign(Check.LOUPE, "Natural diamonds tend to show tiny crystals of other minerals, feathers and clouds rather than metal."),
                GemSign(Check.TOOL, "A strong magnet that tugs the loose stone points to metallic HPHT inclusions."),
                GemSign(Check.UV, "Glowing after the UV light switches off (phosphorescence), or orange to red fluorescence, is more typical of lab diamonds. Many natural diamonds glow blue, but many don't glow at all, so absence proves nothing."),
                GemSign(Check.LAB, "Telling lab from natural for certain needs lab screening equipment, or a GIA or IGI report."),
            ),
            natural = PriceRange(3_500.0, 6_000.0),
            lab = PriceRange(250.0, 1_500.0),
        ),
        Gem(
            id = "ruby",
            name = "Ruby",
            summary = "Cheap lab ruby has been made for over a century. Look for curved lines and bubbles, and watch for glass-filled stones sold as ruby.",
            signs = listOf(
                GemSign(Check.LOUPE, "Curved growth lines, like the grooves of a record: flame-fusion lab ruby. Natural ruby's growth lines and color bands are straight or angular."),
                GemSign(Check.LOUPE, "Round or stretched gas bubbles: a lab stone, or glass filling in a treated stone."),
                GemSign(Check.LOUPE, "Wispy \"fingerprint\" veils and shiny triangular or hexagonal metal platelets: flux-grown lab ruby."),
                GemSign(Check.LOUPE, "Bubbles in surface-reaching cracks and a blue or orange flash when tilted: lead-glass-filled ruby. It is sold cheaply as \"ruby\" but is part glass and fragile."),
                GemSign(Check.LOUPE, "Fine needle-like \"silk\" and mineral crystals suggest natural, though heat treatment can change them."),
                GemSign(Check.UV, "Lab ruby often glows a very strong red, but so do some natural rubies, so treat it as a weak hint."),
                GemSign(Check.LAB, "Most natural rubies are heat-treated, which is normal. Origin, heat and glass filling need a lab report."),
            ),
            natural = PriceRange(500.0, 10_000.0),
            lab = PriceRange(5.0, 500.0),
        ),
        Gem(
            id = "sapphire",
            name = "Sapphire",
            summary = "Same mineral as ruby, with the same clues. Blue lab sapphire is very cheap, so a bargain clean vivid blue deserves a closer look.",
            signs = listOf(
                GemSign(Check.LOUPE, "Curved color bands or growth lines: flame-fusion lab sapphire. Natural sapphire shows straight, angular, often hexagonal color zoning."),
                GemSign(Check.LOUPE, "Gas bubbles, round or stretched: lab-grown."),
                GemSign(Check.LOUPE, "\"Fingerprint\" flux veils or metallic platelets: flux-grown lab sapphire."),
                GemSign(Check.LOUPE, "Rutile silk, mineral crystals and natural fingerprints point to natural, but heating can alter them."),
                GemSign(Check.UV, "A chalky bluish-white or greenish glow under short-wave UV is typical of blue lab sapphire."),
                GemSign(Check.LAB, "Diffusion treatment, heating and origin need a lab report."),
            ),
            natural = PriceRange(500.0, 12_000.0),
            lab = PriceRange(30.0, 200.0),
        ),
        Gem(
            id = "emerald",
            name = "Emerald",
            summary = "Natural emeralds are almost always visibly included and usually oiled. A clean, vivid, cheap emerald is the red flag.",
            signs = listOf(
                GemSign(Check.EYE, "Eye-clean, intensely green and inexpensive: natural emeralds nearly always show inclusions (the \"jardin\")."),
                GemSign(Check.LOUPE, "Zig-zag or chevron growth patterns: hydrothermal lab emerald."),
                GemSign(Check.LOUPE, "\"Nail-head spicules\": tapered tubes with a tiny crystal at the wide end, typical of hydrothermal lab emerald."),
                GemSign(Check.LOUPE, "Wispy veils or metallic platelets: flux-grown lab emerald."),
                GemSign(Check.LOUPE, "Three-phase inclusions (a pocket holding liquid, a gas bubble and a tiny crystal), mica flakes or pyrite point to natural."),
                GemSign(Check.UV, "A strong red glow under long-wave UV is common in lab emerald. Most natural emeralds glow weakly or not at all."),
                GemSign(Check.LAB, "The amount of oil or resin filling, and origin, need a lab report."),
            ),
            natural = PriceRange(3_000.0, 10_000.0),
            lab = PriceRange(300.0, 1_000.0),
        ),
        Gem(
            id = "moissanite",
            name = "Moissanite",
            summary = "Always lab-made. The question is whether a \"diamond\" is really moissanite, which many basic diamond testers pass.",
            signs = listOf(
                GemSign(Check.LOUPE, "Doubled facet edges: look through the top at an angle toward the back facets. Moissanite shows each edge twice; diamond never does."),
                GemSign(Check.EYE, "Strong rainbow \"disco ball\" flashes, more than a diamond of the same size gives."),
                GemSign(Check.EYE, "Older moissanite often has a faint yellow or green tint."),
                GemSign(Check.LOUPE, "Thin needle-like inclusions running parallel to each other."),
                GemSign(Check.TOOL, "A basic heat-based diamond tester can read moissanite as diamond. A moissanite or multi-tester, which also checks electrical conductivity, tells them apart."),
            ),
            natural = null,
            lab = PriceRange(50.0, 800.0),
        ),
        Gem(
            id = "alexandrite",
            name = "Alexandrite",
            summary = "Changes color: green to bluish green in daylight, red to purple under warm indoor light. Natural stones are rare; many \"alexandrites\" are lab-made or not alexandrite at all.",
            signs = listOf(
                GemSign(Check.EYE, "A large, clean stone with a dramatic color change at a modest price is very likely lab-grown."),
                GemSign(Check.EYE, "Lavender-blue turning to purple-pink is lab color-change sapphire or spinel, often sold as \"alexandrite\". True alexandrite goes green to red."),
                GemSign(Check.LOUPE, "Curved lines and gas bubbles mean color-change lab corundum, not alexandrite."),
                GemSign(Check.LOUPE, "Metallic platelets or flux veils: flux-grown lab alexandrite. Very clean stones may be pulled lab crystals."),
                GemSign(Check.LOUPE, "Fine needles, fingerprints and step-like twinning lines point to natural."),
                GemSign(Check.LAB, "Natural alexandrite is valuable enough that a lab report is worth getting."),
            ),
            natural = PriceRange(500.0, 10_000.0),
            lab = null,
        ),
        Gem(
            id = "spinel",
            name = "Spinel",
            summary = "Natural spinel is prized in its own right. Lab spinel is cheap and is also used to imitate aquamarine and blue sapphire.",
            signs = listOf(
                GemSign(Check.LOUPE, "Gas bubbles: flame-fusion lab spinel."),
                GemSign(Check.LOUPE, "Rows of tiny octahedral crystals, like little pyramids point to point, are classic natural spinel."),
                GemSign(Check.TOOL, "Under a polariscope, a criss-cross \"tatami\" strain pattern suggests lab spinel. Natural spinel usually stays dark."),
                GemSign(Check.EYE, "A cheap \"aquamarine\" or \"blue sapphire\" that is unusually bright may be lab spinel."),
                GemSign(Check.LAB, "Fine natural spinel (red, pink, cobalt blue) is worth a lab report."),
            ),
            natural = null,
            lab = null,
        ),
        Gem(
            id = "opal",
            name = "Opal",
            summary = "Lab opal, plastic and glass imitations, and thin slices glued onto a backing (doublets and triplets) are all common.",
            signs = listOf(
                GemSign(Check.LOUPE, "A \"lizard skin\", snakeskin or honeycomb pattern inside each color patch, too regular and repeating: lab (Gilson-type) opal."),
                GemSign(Check.EYE, "Look at the side: a straight join line and a dark backing mean a doublet. A clear domed cap on top means a triplet. Both are worth far less than solid opal."),
                GemSign(Check.EYE, "Very light and warm to the touch: plastic imitation. Glass imitations show bubbles and swirls."),
                GemSign(Check.EYE, "Natural opal's color patches are irregular and change as you tilt it, often with patches of plain \"potch\"."),
                GemSign(Check.LAB, "Telling some lab opals from natural without magnification work, and checking for treatments, needs a lab."),
            ),
            natural = null,
            lab = null,
        ),
        Gem(
            id = "quartz",
            name = "Quartz / Amethyst",
            summary = "Lab amethyst and citrine are common and chemically identical to natural. Prices are low enough that testing is rarely worth it.",
            signs = listOf(
                GemSign(Check.EYE, "Perfectly even color with no zoning at all is a hint toward lab-grown."),
                GemSign(Check.LOUPE, "Angular color bands, \"tiger stripe\" or \"zebra\" zoning and natural fingerprints point to natural amethyst."),
                GemSign(Check.LOUPE, "\"Breadcrumb\" specks or a visible flat seed plate: lab-grown quartz."),
                GemSign(Check.EYE, "Most citrine sold is heated amethyst, giving a strong orange-brown or reddish color. Natural citrine is usually pale yellow."),
                GemSign(Check.LAB, "A clean stone can only be confirmed in a lab, which is rarely worth it at these prices."),
            ),
            natural = PriceRange(2.0, 60.0),
            lab = null,
        ),
    )

    fun byId(id: String): Gem = all.firstOrNull { it.id == id } ?: all.first()
}
