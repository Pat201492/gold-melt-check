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

/**
 * A gem mined in Sri Lanka. [prices] holds one USD-per-carat ballpark per [Bracket] (in bracket order):
 * for stones where heat treatment changes the price these are heated stones, and [unheatedPrices] holds the
 * unheated ones; otherwise [unheatedPrices] is null. A null entry means no reliable figure was found.
 */
data class Gem(
    val id: String,
    val name: String,
    val summary: String,
    val signs: List<GemSign>,
    val prices: List<PriceRange?>,
    val unheatedPrices: List<PriceRange?>? = null,
) {
    val heatMatters: Boolean get() = unheatedPrices != null

    /** The default ballpark for a bracket. Unknown treatment is compared with heated prices, the common case. */
    fun range(bracket: Bracket, heat: Heat): PriceRange? =
        if (heat == Heat.UNHEATED && unheatedPrices != null) unheatedPrices[bracket.ordinal] else prices[bracket.ordinal]
}

private fun r(low: Double, high: Double) = PriceRange(low, high)
private val none: PriceRange? = null

/**
 * Sri Lankan gems: deal ballparks and signs of lab-grown or imitation stones.
 *
 * Prices are rough asking prices from Sri Lankan dealers and price guides, USD per carat, compiled Nov 2026.
 * Street prices in Ratnapura can be lower and tourist-shop prices much higher. Signs are hints, not proof.
 */
object Gems {
    /** When the price ballparks were compiled. Update all three together whenever the ranges change. */
    const val PRICES_YEAR = 2026
    const val PRICES_MONTH = 11
    const val PRICES_AS_OF = "Nov 2026"

    /** Ballparks older than this many months get a warning in the app. */
    const val STALE_AFTER_MONTHS = 6

    /** Whole months between the ballpark date and [year]/[month] (1-12). */
    fun ageInMonths(year: Int, month: Int): Int = (year - PRICES_YEAR) * 12 + (month - PRICES_MONTH)

    fun isStale(year: Int, month: Int): Boolean = ageInMonths(year, month) >= STALE_AFTER_MONTHS

    /** Buying in Sri Lanka: applies to every stone. */
    val general = listOf(
        GemSign(Check.LAB, "Ask for a certificate from the NGJA (National Gem & Jewellery Authority) gem testing lab in Colombo, or have the stone tested there before paying for anything valuable. A dealer who refuses is a warning sign."),
        GemSign(Check.EYE, "Buy from NGJA-licensed dealers and keep the official receipt. You'll want it, and the certificate, if you take the stone out of the country."),
        GemSign(Check.EYE, "\"Unheated\" is the most common false claim, and it can double or triple the price. Only a lab report settles it."),
        GemSign(Check.EYE, "Walk away from street touts, \"mine visits\" with a surprise sale at the end, and offers to resell the stone abroad for a profit."),
        GemSign(Check.EYE, "Geuda is milky low-grade sapphire that is heated to turn blue. It's legitimate when sold as heated, but it's often passed off as natural unheated."),
        GemSign(Check.LAB, "Lab-grown stones are the same material as natural ones and pass standard testers. Glass and synthetic sapphire imitations are common in tourist areas."),
    )

    val all = listOf(
        Gem(
            id = "blue_sapphire",
            name = "Blue sapphire",
            summary = "Ceylon blue is lighter and brighter than most other origins. Most is heated; unheated, certified stones cost 2–3× more.",
            signs = listOf(
                GemSign(Check.LOUPE, "Curved growth lines or round gas bubbles: lab-made (flame-fusion) sapphire. Natural growth lines are straight or angular."),
                GemSign(Check.LOUPE, "Fine needle-like \"silk\" in a natural pattern points to natural and often unheated. Heating dissolves silk into dots or makes it vanish."),
                GemSign(Check.LOUPE, "Glass-like filling in surface-reaching cracks, with bubbles or a flash when tilted: a glass-filled stone, worth far less."),
                GemSign(Check.EYE, "Perfectly even, inky colour with no zoning in a cheap stone: possibly synthetic or not Ceylon."),
                GemSign(Check.LAB, "Heated vs unheated, and Ceylon vs other origins, need a lab (NGJA, GIA, GRS)."),
            ),
            prices = listOf(r(150.0, 1_000.0), r(300.0, 3_000.0), r(500.0, 4_000.0), r(800.0, 8_000.0), r(1_500.0, 10_000.0), r(2_500.0, 15_000.0)),
            unheatedPrices = listOf(r(400.0, 2_000.0), r(1_000.0, 5_000.0), r(1_500.0, 7_000.0), r(2_500.0, 10_000.0), r(4_000.0, 18_000.0), r(6_000.0, 25_000.0)),
        ),
        Gem(
            id = "padparadscha",
            name = "Padparadscha",
            summary = "Sri Lanka's pink-orange \"lotus\" sapphire, the most valuable sapphire colour. Prices rise very steeply above 2 ct.",
            signs = listOf(
                GemSign(Check.EYE, "Too orange, too pink, or colour that sits only on the surface: likely beryllium-treated or not true padparadscha."),
                GemSign(Check.LOUPE, "Colour concentrated in a thin outer layer seen from the side: surface diffusion treatment."),
                GemSign(Check.LOUPE, "Curved growth lines or gas bubbles: lab-made."),
                GemSign(Check.LAB, "Padparadscha is one of the most faked stones. Insist on a lab report that names the colour \"padparadscha\" and states the treatment."),
            ),
            prices = listOf(r(800.0, 3_000.0), r(1_500.0, 5_000.0), r(2_000.0, 6_000.0), r(2_500.0, 8_000.0), r(3_000.0, 10_000.0), none),
            unheatedPrices = listOf(r(1_500.0, 5_000.0), r(2_500.0, 10_000.0), r(4_000.0, 15_000.0), r(6_000.0, 25_000.0), r(8_000.0, 35_000.0), none),
        ),
        Gem(
            id = "yellow_sapphire",
            name = "Yellow sapphire",
            summary = "Popular in Sri Lanka and India (pukhraj). Conventional heating is common; beryllium-treated stones are far cheaper.",
            signs = listOf(
                GemSign(Check.EYE, "Very bright orange-yellow at a low price: often beryllium-treated, worth a fraction of conventionally heated stones."),
                GemSign(Check.LOUPE, "Curved lines or gas bubbles: lab-made."),
                GemSign(Check.TOOL, "Yellow glass and citrine are sold as yellow sapphire. A refractometer or the NGJA lab tells them apart quickly."),
                GemSign(Check.LAB, "Unheated and beryllium-free status needs a lab report."),
            ),
            prices = listOf(r(150.0, 600.0), r(350.0, 1_000.0), r(400.0, 1_200.0), r(500.0, 1_500.0), r(600.0, 2_000.0), r(600.0, 2_500.0)),
            unheatedPrices = listOf(r(400.0, 1_500.0), r(800.0, 3_500.0), r(1_200.0, 5_000.0), r(1_500.0, 8_000.0), r(1_800.0, 10_000.0), r(2_000.0, 12_000.0)),
        ),
        Gem(
            id = "pink_sapphire",
            name = "Pink sapphire",
            summary = "Ceylon pinks are usually light to medium pink. Beryllium-treated pinks sell for under \$10 a carat.",
            signs = listOf(
                GemSign(Check.EYE, "Very cheap, intensely coloured pink: likely beryllium-treated or synthetic."),
                GemSign(Check.LOUPE, "Curved lines or gas bubbles: lab-made."),
                GemSign(Check.LAB, "Ruby vs pink sapphire, and treatment, need a lab report."),
            ),
            prices = listOf(r(150.0, 800.0), r(300.0, 1_500.0), r(500.0, 2_000.0), r(700.0, 2_500.0), r(800.0, 3_500.0), none),
            unheatedPrices = listOf(r(500.0, 1_500.0), r(1_100.0, 2_900.0), r(1_200.0, 3_500.0), r(1_500.0, 4_500.0), r(2_000.0, 6_000.0), none),
        ),
        Gem(
            id = "white_sapphire",
            name = "White sapphire",
            summary = "Colourless sapphire, valued for clarity and cut. Often sold as a diamond alternative.",
            signs = listOf(
                GemSign(Check.EYE, "Lots of rainbow fire: likely cubic zirconia or glass. Sapphire shows little fire."),
                GemSign(Check.LOUPE, "Rounded facet edges and gas bubbles: glass."),
                GemSign(Check.TOOL, "White topaz and quartz are swapped in. A refractometer or the NGJA lab tells them apart."),
            ),
            prices = listOf(r(50.0, 300.0), r(100.0, 600.0), r(150.0, 800.0), r(200.0, 1_000.0), r(300.0, 1_500.0), r(300.0, 2_000.0)),
        ),
        Gem(
            id = "star_sapphire",
            name = "Star sapphire",
            summary = "Cut as a dome to show a six-rayed star. Priced on how sharp and centred the star is.",
            signs = listOf(
                GemSign(Check.EYE, "A perfectly sharp, bright star on a very even body colour: typical of lab-made (Linde-type) star sapphire."),
                GemSign(Check.EYE, "A star that looks painted on, or stays put when you move the light: imitation or backed stone."),
                GemSign(Check.LOUPE, "Gas bubbles or curved lines in the dome: lab-made."),
            ),
            prices = listOf(r(50.0, 500.0), r(100.0, 1_500.0), r(100.0, 2_000.0), r(150.0, 3_000.0), r(200.0, 5_000.0), none),
        ),
        Gem(
            id = "ruby",
            name = "Ruby",
            summary = "Sri Lankan rubies tend toward lighter pinkish-red. Most are heated; glass-filled rubies are common and worth little.",
            signs = listOf(
                GemSign(Check.LOUPE, "Bubbles in cracks and a blue or orange flash when tilted: lead-glass-filled. Fragile and worth a small fraction of real ruby."),
                GemSign(Check.LOUPE, "Curved growth lines or gas bubbles: lab-made."),
                GemSign(Check.UV, "A very strong red glow is typical of lab ruby, but some natural rubies glow too."),
                GemSign(Check.LAB, "Heating, glass filling and origin need a lab report."),
            ),
            prices = listOf(r(100.0, 400.0), r(150.0, 800.0), r(300.0, 2_000.0), r(500.0, 3_000.0), r(800.0, 4_000.0), none),
            unheatedPrices = listOf(r(400.0, 2_000.0), r(1_000.0, 6_000.0), r(3_000.0, 15_000.0), r(3_000.0, 15_000.0), r(4_000.0, 20_000.0), none),
        ),
        Gem(
            id = "cats_eye",
            name = "Cat's eye (chrysoberyl)",
            summary = "Sri Lanka is the classic source. The finest show a sharp eye on a \"milk and honey\" body. Eye sharpness matters more than size.",
            signs = listOf(
                GemSign(Check.EYE, "Visible parallel fibres or a honeycomb pattern on the sides: glass fibre-optic imitation."),
                GemSign(Check.EYE, "A cheap, greenish or greyish stone sold as \"cat's eye\": often quartz cat's eye, worth far less than chrysoberyl."),
                GemSign(Check.EYE, "Shine a light from one side: in fine chrysoberyl one half glows milky and the other stays honey-coloured."),
                GemSign(Check.TOOL, "Chrysoberyl feels heavy for its size. A scale or the NGJA lab separates it from quartz."),
            ),
            prices = listOf(r(100.0, 400.0), r(100.0, 600.0), r(300.0, 2_500.0), r(500.0, 2_500.0), r(1_500.0, 10_000.0), r(1_500.0, 12_000.0)),
        ),
        Gem(
            id = "alexandrite",
            name = "Alexandrite",
            summary = "Changes colour from green in daylight to red or purple under warm light. Ceylon alexandrite is rare; many \"alexandrites\" are lab-made.",
            signs = listOf(
                GemSign(Check.EYE, "Lavender-blue turning purple-pink: lab colour-change sapphire or spinel, not alexandrite."),
                GemSign(Check.EYE, "A large, clean stone with a dramatic colour change at a modest price: very likely lab-grown."),
                GemSign(Check.LOUPE, "Curved lines and gas bubbles: lab colour-change corundum."),
                GemSign(Check.LAB, "Natural alexandrite is valuable enough that a lab report is worth getting."),
            ),
            prices = listOf(r(500.0, 3_000.0), r(1_000.0, 6_000.0), r(1_500.0, 8_000.0), r(2_000.0, 10_000.0), r(3_000.0, 15_000.0), none),
        ),
        Gem(
            id = "spinel",
            name = "Spinel",
            summary = "Sri Lanka produces pink, purple, red and rare cobalt-blue spinel. Cobalt blue sells far above the ranges here.",
            signs = listOf(
                GemSign(Check.LOUPE, "Gas bubbles: lab-made (flame-fusion) spinel."),
                GemSign(Check.LOUPE, "Tiny octahedral crystals, often in rows: classic natural spinel."),
                GemSign(Check.EYE, "A cheap \"blue sapphire\" or \"aquamarine\" with unusual brightness may be lab spinel."),
            ),
            prices = listOf(r(100.0, 800.0), r(200.0, 1_500.0), r(300.0, 2_000.0), r(400.0, 2_500.0), r(600.0, 3_500.0), none),
        ),
        Gem(
            id = "moonstone",
            name = "Moonstone",
            summary = "Sri Lanka's best moonstone shows a floating blue sheen. Most is inexpensive; strong blue flash costs more.",
            signs = listOf(
                GemSign(Check.EYE, "An even bluish glow across the whole stone, the same from every angle: glass \"opalite\", not moonstone."),
                GemSign(Check.EYE, "Real moonstone's sheen moves and shows only at certain angles."),
                GemSign(Check.LOUPE, "Gas bubbles or swirls: glass."),
            ),
            prices = listOf(r(5.0, 40.0), r(7.0, 60.0), r(10.0, 100.0), r(20.0, 200.0), r(30.0, 300.0), r(30.0, 300.0)),
        ),
        Gem(
            id = "hessonite",
            name = "Garnet (hessonite)",
            summary = "Cinnamon-orange garnet (gomedh), common in Sri Lanka. Orange spessartite garnet costs more.",
            signs = listOf(
                GemSign(Check.LOUPE, "A wavy, \"heat haze\" look inside is typical of natural hessonite."),
                GemSign(Check.LOUPE, "Gas bubbles and rounded facet edges: glass."),
            ),
            prices = listOf(r(20.0, 150.0), r(50.0, 300.0), r(50.0, 300.0), r(50.0, 300.0), r(50.0, 300.0), r(50.0, 300.0)),
        ),
        Gem(
            id = "zircon",
            name = "Zircon",
            summary = "Natural zircon, not cubic zirconia. Bright blue zircon is heat-treated as standard.",
            signs = listOf(
                GemSign(Check.LOUPE, "Doubled facet edges seen through the top: zircon. Glass and cubic zirconia never show it."),
                GemSign(Check.EYE, "Strong rainbow fire on a cheap \"zircon\": check it isn't cubic zirconia."),
            ),
            prices = listOf(r(20.0, 150.0), r(50.0, 250.0), r(50.0, 300.0), r(50.0, 300.0), r(100.0, 500.0), r(100.0, 500.0)),
        ),
        Gem(
            id = "topaz",
            name = "Topaz",
            summary = "Mostly colourless or pale. Bright \"London\" or \"Swiss\" blue topaz is irradiated, which is normal.",
            signs = listOf(
                GemSign(Check.LOUPE, "Gas bubbles and rounded facet edges: glass."),
                GemSign(Check.TOOL, "Topaz is sometimes sold as aquamarine or white sapphire. The NGJA lab separates them quickly."),
            ),
            prices = listOf(r(3.0, 15.0), r(5.0, 20.0), r(5.0, 25.0), r(5.0, 30.0), r(5.0, 30.0), r(5.0, 40.0)),
        ),
        Gem(
            id = "tourmaline",
            name = "Tourmaline",
            summary = "Sri Lanka produces yellow, brown and green tourmaline in smaller amounts.",
            signs = listOf(
                GemSign(Check.LOUPE, "Doubled facet edges in larger stones point to tourmaline; glass shows none."),
                GemSign(Check.LOUPE, "Gas bubbles and swirls: glass."),
            ),
            prices = listOf(r(20.0, 150.0), r(30.0, 250.0), r(40.0, 300.0), r(50.0, 400.0), r(60.0, 500.0), r(60.0, 500.0)),
        ),
        Gem(
            id = "amethyst",
            name = "Amethyst",
            summary = "Purple quartz, found in Sri Lanka's gem gravels. Inexpensive; deep, even purple without brown or grey costs the most.",
            signs = listOf(
                GemSign(Check.EYE, "Patchy or banded purple is normal in natural amethyst. Perfectly even colour in a large, flawless stone may be lab-grown quartz."),
                GemSign(Check.LOUPE, "Gas bubbles, swirls and rounded facet edges: glass."),
                GemSign(Check.EYE, "Amethyst is cheap. A dealer pricing it like sapphire, or calling it \"purple sapphire\", is a warning sign. The NGJA lab separates them quickly."),
            ),
            prices = listOf(r(3.0, 15.0), r(5.0, 20.0), r(5.0, 25.0), r(5.0, 25.0), r(5.0, 30.0), r(5.0, 30.0)),
        ),
        Gem(
            id = "aquamarine",
            name = "Aquamarine",
            summary = "Pale blue to blue-green beryl, found in smaller amounts in Sri Lanka.",
            signs = listOf(
                GemSign(Check.TOOL, "Blue topaz and lab spinel are sold as aquamarine. A refractometer or the NGJA lab tells them apart."),
                GemSign(Check.LOUPE, "Gas bubbles: glass or lab spinel."),
            ),
            prices = listOf(r(20.0, 120.0), r(30.0, 250.0), r(40.0, 300.0), r(50.0, 400.0), r(60.0, 500.0), r(60.0, 500.0)),
        ),
    )

    fun byId(id: String): Gem = all.firstOrNull { it.id == id } ?: all.first()
}
