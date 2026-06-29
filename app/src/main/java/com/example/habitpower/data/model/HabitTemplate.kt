package com.example.habitpower.data.model

data class HabitTemplate(
    val name: String,
    val description: String,
    val identityStatement: String,
    val type: HabitType,
    val unit: String? = null,
    val targetValue: Double? = null,
    val commitmentHour: Int = 8,
    val commitmentMinute: Int = 0,
    val archetype: Archetype
) {
    enum class Archetype(val label: String, val tagline: String) {
        HUMAN(
            "Human",
            "The biological foundation every person needs — independent of goals, job, or lifestyle."
        ),
        BUILDER(
            "Builder",
            "For makers, entrepreneurs, and creatives building something that matters."
        ),
        ATHLETE(
            "Athlete",
            "For those treating physical performance as a daily discipline."
        ),
        STUDENT(
            "Student",
            "For learners protecting focus and building knowledge daily."
        ),
        MINDFUL(
            "Mindful",
            "For those prioritising inner life, presence, and emotional depth."
        )
    }
}

object HabitTemplates {
    val all: List<HabitTemplate> = listOf(

        // ── Human ─────────────────────────────────────────────────────────────
        // The biological defaults that every person needs regardless of archetype.
        // These are not aspirational — they are the operating system of a healthy life.
        HabitTemplate(
            name = "Cold Shower",
            description = "Two minutes of cold water raises dopamine by 250% for hours, reduces inflammation, and builds stress resilience that carries over to every hard thing in the day. The hardest-easiest habit there is.",
            identityStatement = "chooses deliberate discomfort, so difficulty loses its power over me",
            type = HabitType.BOOLEAN,
            commitmentHour = 7, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.HUMAN
        ),
        HabitTemplate(
            name = "Phone-Free Meal",
            description = "One meal a day without a screen. Scrolling while eating triples cortisol, halves the enjoyment signal, and trains the brain to need stimulation to function. Presence at one meal a day changes your relationship with both food and your phone.",
            identityStatement = "eats with full presence — tasting the food, not consuming content",
            type = HabitType.BOOLEAN,
            commitmentHour = 13, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.HUMAN
        ),
        HabitTemplate(
            name = "Real Conversation",
            description = "One meaningful exchange daily — in person or by call, not a text thread. Holt-Lunstad's meta-analysis of 3.4 million people found social isolation increases early mortality risk by 50%. Connection is not optional for a well-functioning life.",
            identityStatement = "invests daily in the relationships that make a life worth living",
            type = HabitType.BOOLEAN,
            commitmentHour = 19, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.HUMAN
        ),
        HabitTemplate(
            name = "Digital Sunset",
            description = "All screens away by 9pm. Blue light from screens after dark suppresses melatonin for 3+ hours, delaying sleep onset. This single boundary can recover 45 minutes of sleep quality per night — without sleeping longer.",
            identityStatement = "creates a nightly transition from the world's demands to genuine rest",
            type = HabitType.BOOLEAN,
            commitmentHour = 21, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.HUMAN
        ),
        HabitTemplate(
            name = "Whole-Food Meal",
            description = "One meal from real ingredients daily — cooked or assembled, not from a package. Ultra-processed food is now independently linked to depression, cognitive decline, and chronic inflammation. One real meal is the minimum standard of nutritional self-respect.",
            identityStatement = "feeds my body food that was once alive",
            type = HabitType.BOOLEAN,
            commitmentHour = 12, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.HUMAN
        ),
        HabitTemplate(
            name = "Evening Walk",
            description = "15 minutes of light walking after dinner reduces blood glucose spike by 30%, supports digestion, and signals the nervous system to shift from stimulation to rest. The Japanese practice this as sanpo — a purposeless walk. That purposelessness is the point.",
            identityStatement = "transitions from the day's doing to the evening's being with a walk",
            type = HabitType.DURATION,
            unit = "mins",
            targetValue = 15.0,
            commitmentHour = 20, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.HUMAN
        ),
        HabitTemplate(
            name = "Strength Training",
            description = "Resistance training 3x/week reduces all-cause mortality by 40% and is the single best intervention for metabolic health across every decade of life. Peter Attia calls muscle the longevity organ. This is non-negotiable at any age.",
            identityStatement = "builds a body designed to serve me through the decades, not just the years",
            type = HabitType.BOOLEAN,
            commitmentHour = 6, commitmentMinute = 30,
            archetype = HabitTemplate.Archetype.HUMAN
        ),
        HabitTemplate(
            name = "Sit in Silence",
            description = "10 minutes of complete silence — no podcast, no music, no input. Silence is now the rarest resource in the modern world. The mind needs unstructured time to consolidate, process, and restore what constant input never allows.",
            identityStatement = "creates space for silence in a world that never stops talking",
            type = HabitType.DURATION,
            unit = "mins",
            targetValue = 10.0,
            commitmentHour = 7, commitmentMinute = 30,
            archetype = HabitTemplate.Archetype.HUMAN
        ),

        // ── Builder ───────────────────────────────────────────────────────────
        HabitTemplate(
            name = "Deep Work",
            description = "Uninterrupted, distraction-free focus on your most important project. Your best work happens here.",
            identityStatement = "gives my best hours to meaningful work",
            type = HabitType.DURATION,
            unit = "mins",
            targetValue = 90.0,
            commitmentHour = 9, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.BUILDER
        ),
        HabitTemplate(
            name = "Daily Reading",
            description = "30 minutes a day compounds into a book a week over a year. Readers are leaders.",
            identityStatement = "reads every day to grow my mind",
            type = HabitType.DURATION,
            unit = "mins",
            targetValue = 30.0,
            commitmentHour = 21, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.BUILDER
        ),
        HabitTemplate(
            name = "Ship Something",
            description = "Publish, release, send. Shipping is the only feedback loop that matters.",
            identityStatement = "ships, not just plans",
            type = HabitType.BOOLEAN,
            commitmentHour = 18, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.BUILDER
        ),
        HabitTemplate(
            name = "No Phone First Hour",
            description = "The first hour sets the nervous system tone for the whole day. Own your morning before the world does.",
            identityStatement = "owns my mornings",
            type = HabitType.BOOLEAN,
            commitmentHour = 6, commitmentMinute = 30,
            archetype = HabitTemplate.Archetype.BUILDER
        ),

        // ── Athlete ───────────────────────────────────────────────────────────
        HabitTemplate(
            name = "Morning Training",
            description = "Morning movement primes your brain for the day and builds the discipline that transfers everywhere else.",
            identityStatement = "trains my body as a daily discipline",
            type = HabitType.DURATION,
            unit = "mins",
            targetValue = 45.0,
            commitmentHour = 6, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.ATHLETE
        ),
        HabitTemplate(
            name = "Daily Steps",
            description = "Compound daily movement is the single best-researched longevity habit. 10,000 steps is the floor.",
            identityStatement = "moves every day without exception",
            type = HabitType.COUNT,
            unit = "steps",
            targetValue = 10000.0,
            commitmentHour = 19, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.ATHLETE
        ),
        HabitTemplate(
            name = "Hydration",
            description = "Starting with 500ml rehydrates your brain before it meets the day. Keep going through it.",
            identityStatement = "takes care of my body from the inside out",
            type = HabitType.COUNT,
            unit = "glasses",
            targetValue = 8.0,
            commitmentHour = 12, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.ATHLETE
        ),
        HabitTemplate(
            name = "Sleep 7h+",
            description = "Sleep is not recovery. Sleep is performance. Non-negotiable minimum: 7 hours.",
            identityStatement = "protects my sleep as a foundation for everything else",
            type = HabitType.DURATION,
            unit = "mins",
            targetValue = 420.0,
            commitmentHour = 22, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.ATHLETE
        ),
        HabitTemplate(
            name = "Morning Sunlight",
            description = "10 minutes of outdoor light within an hour of waking anchors your circadian rhythm and lifts energy all day.",
            identityStatement = "starts the day with light before screens",
            type = HabitType.BOOLEAN,
            commitmentHour = 7, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.ATHLETE
        ),

        // ── Student ───────────────────────────────────────────────────────────
        HabitTemplate(
            name = "Study Block",
            description = "Deep study in focused rounds beats hours of scattered browsing. One Pomodoro at a time.",
            identityStatement = "studies with full presence, not just time on task",
            type = HabitType.POMODORO,
            unit = "sessions",
            targetValue = 4.0,
            commitmentHour = 10, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.STUDENT
        ),
        HabitTemplate(
            name = "Flashcard Review",
            description = "Spaced repetition is the most efficient learning system known. 10 minutes daily beats 2-hour cramming.",
            identityStatement = "reviews what I've learned so it actually sticks",
            type = HabitType.BOOLEAN,
            commitmentHour = 8, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.STUDENT
        ),
        HabitTemplate(
            name = "No Social Media After 10pm",
            description = "Late screen use fractures sleep and breeds comparison anxiety. Your future self will thank you.",
            identityStatement = "protects my sleep and focus by setting a digital curfew",
            type = HabitType.BOOLEAN,
            commitmentHour = 22, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.STUDENT
        ),

        // ── Mindful ───────────────────────────────────────────────────────────
        HabitTemplate(
            name = "Morning Meditation",
            description = "Stillness before the day's noise begins. Even 10 minutes changes the quality of everything that follows.",
            identityStatement = "starts each day with stillness before the world begins",
            type = HabitType.DURATION,
            unit = "mins",
            targetValue = 20.0,
            commitmentHour = 6, commitmentMinute = 30,
            archetype = HabitTemplate.Archetype.MINDFUL
        ),
        HabitTemplate(
            name = "Gratitude Journal",
            description = "Three specific things, written. Not typed. The act of writing is the practice.",
            identityStatement = "notices what's good in my life every single day",
            type = HabitType.TEXT,
            commitmentHour = 21, commitmentMinute = 30,
            archetype = HabitTemplate.Archetype.MINDFUL
        ),
        HabitTemplate(
            name = "Walk Outside",
            description = "Walking without a destination or podcast resets the nervous system. Presence is the practice.",
            identityStatement = "takes time to be present in the world outside my screen",
            type = HabitType.BOOLEAN,
            commitmentHour = 17, commitmentMinute = 0,
            archetype = HabitTemplate.Archetype.MINDFUL
        ),
        HabitTemplate(
            name = "Breath Work",
            description = "5 minutes of box breathing or 4-7-8 breathing activates the parasympathetic nervous system. Instant reset.",
            identityStatement = "regulates my nervous system intentionally",
            type = HabitType.DURATION,
            unit = "mins",
            targetValue = 5.0,
            commitmentHour = 7, commitmentMinute = 30,
            archetype = HabitTemplate.Archetype.MINDFUL
        )
    )

    fun byArchetype(): Map<HabitTemplate.Archetype, List<HabitTemplate>> =
        HabitTemplate.Archetype.entries.associateWith { archetype ->
            all.filter { it.archetype == archetype }
        }
}
