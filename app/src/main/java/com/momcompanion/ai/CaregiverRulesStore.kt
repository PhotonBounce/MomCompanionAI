package com.friendai

import android.content.Context

data class CaregiverSettings(
    val rules: String,
    val profileNotes: String,
    val vocabularyNotes: String,
    val promptTopics: String,
    val pin: String,
    val contactName: String,
    val contactPhone: String,
    val backendUrl: String,
    val backendToken: String,
    val setupComplete: Boolean = false,
    /**
     * Hours (1-12) the app actively listens for Mom without requiring her to press
     * anything. This is the CORE interaction — the target user cannot operate buttons,
     * so hands-free listening is ON by default (12h). The caregiver can set 0 in
     * Settings to fall back to a push-to-talk-only mode if ever needed.
     */
    val timedListeningHours: Int = CaregiverRulesStore.DEFAULT_LISTENING_HOURS,
    /**
     * TTS speech rate multiplier. 1.0 = normal, 0.7 = slow (easier to follow for elderly /
     * dementia patients), 1.3 = faster. Applied to both push-to-talk replies and the
     * timed-listening service. Caregiver-configurable in Settings.
     */
    val ttsSpeechRate: Float = 1.0f,
    /**
     * Optional free Gemini API key (from aistudio.google.com/apikey). When set, the app
     * calls Gemini directly for real AI conversation — no separate server needed.
     */
    val geminiApiKey: String = ""
)

class CaregiverRulesStore(private val context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): CaregiverSettings {
        return CaregiverSettings(
            rules = preferences.getString(KEY_RULES, DEFAULT_RULES).orEmpty(),
            profileNotes = preferences.getString(KEY_PROFILE_NOTES, DEFAULT_PROFILE_NOTES).orEmpty(),
            vocabularyNotes = preferences.getString(KEY_VOCABULARY_NOTES, DEFAULT_VOCABULARY_NOTES).orEmpty(),
            promptTopics = preferences.getString(KEY_PROMPT_TOPICS, DEFAULT_PROMPT_TOPICS).orEmpty(),
            pin = preferences.getString(KEY_PIN, DEFAULT_PIN).orEmpty(),
            contactName = preferences.getString(KEY_CONTACT_NAME, DEFAULT_CONTACT_NAME).orEmpty(),
            contactPhone = preferences.getString(KEY_CONTACT_PHONE, DEFAULT_CONTACT_PHONE).orEmpty(),
            backendUrl = preferences.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL).orEmpty(),
            backendToken = preferences.getString(KEY_BACKEND_TOKEN, DEFAULT_BACKEND_TOKEN).orEmpty(),
            setupComplete = preferences.getBoolean(KEY_SETUP_COMPLETE, false),
            timedListeningHours = preferences.getInt(KEY_TIMED_LISTENING_HOURS, DEFAULT_LISTENING_HOURS).coerceIn(0, 12),
            ttsSpeechRate = preferences.getFloat(KEY_TTS_SPEECH_RATE, DEFAULT_TTS_SPEECH_RATE).coerceIn(0.5f, 2.0f),
            geminiApiKey = preferences.getString(KEY_GEMINI_API_KEY, "").orEmpty()
        )
    }

    fun save(settings: CaregiverSettings) {
        preferences
            .edit()
            .putString(KEY_RULES, settings.rules.trim())
            .putString(KEY_PROFILE_NOTES, settings.profileNotes.trim())
            .putString(KEY_VOCABULARY_NOTES, settings.vocabularyNotes.trim())
            .putString(KEY_PROMPT_TOPICS, settings.promptTopics.trim())
            .putString(KEY_PIN, settings.pin.trim().ifBlank { DEFAULT_PIN })
            .putString(KEY_CONTACT_NAME, settings.contactName.trim())
            .putString(KEY_CONTACT_PHONE, settings.contactPhone.trim())
            .putString(KEY_BACKEND_URL, settings.backendUrl.trim())
            .putString(KEY_BACKEND_TOKEN, settings.backendToken.trim())
            .putBoolean(KEY_SETUP_COMPLETE, settings.setupComplete)
            .putInt(KEY_TIMED_LISTENING_HOURS, settings.timedListeningHours.coerceIn(0, 12))
            .putFloat(KEY_TTS_SPEECH_RATE, settings.ttsSpeechRate.coerceIn(0.5f, 2.0f))
            .putString(KEY_GEMINI_API_KEY, settings.geminiApiKey.trim())
            .apply()
    }

    fun isPinValid(pin: String): Boolean {
        return pin == load().pin
    }

    fun isSetupComplete(): Boolean {
        return load().setupComplete
    }

    fun resetRulesAndProfile() {
        val current = load()
        save(
            current.copy(
                rules = DEFAULT_RULES,
                profileNotes = DEFAULT_PROFILE_NOTES,
                vocabularyNotes = DEFAULT_VOCABULARY_NOTES,
                promptTopics = DEFAULT_PROMPT_TOPICS
            )
        )
    }

    companion object {
                private const val PREFERENCES_NAME = "caregiver_rules"
                private const val KEY_RULES = "rules"
                private const val KEY_PROFILE_NOTES = "profile_notes"
                private const val KEY_VOCABULARY_NOTES = "vocabulary_notes"
                private const val KEY_PROMPT_TOPICS = "prompt_topics"
                private const val KEY_PIN = "pin"
                private const val KEY_CONTACT_NAME = "contact_name"
                private const val KEY_CONTACT_PHONE = "contact_phone"
                private const val KEY_BACKEND_URL = "backend_url"
                private const val KEY_BACKEND_TOKEN = "backend_token"
                private const val KEY_SETUP_COMPLETE = "setup_complete"
                private const val KEY_GEMINI_API_KEY = "gemini_api_key"
                private const val KEY_TIMED_LISTENING_HOURS = "timed_listening_hours"
                private const val KEY_TTS_SPEECH_RATE = "tts_speech_rate"
                private const val DEFAULT_TTS_SPEECH_RATE = 1.0f

                val DEFAULT_RULES = """
                    1. Always speak with warmth and kindness. Never scold or rush.
                    2. Keep every reply short — 2 to 4 sentences. Replies are read aloud.
                    3. Gently remind her to drink water, eat, and take her medication.
                    4. If she repeats herself, respond as warmly as if you're hearing it for the first time.
                    5. Never correct her if she misremembers — gently redirect or go along with it.
                    6. If she seems disoriented about the day or time, calmly orient her without making her feel bad.
                    7. When she shares a memory, show genuine curiosity and ask one follow-up question.
                    8. Offer a joke or a short story when the conversation is quiet.
                    9. If she is sad or lonely, validate her feelings first, then offer warmth and presence.
                    10. Her family loves her and visits regularly — remind her of this when she seems down.
                    11. If she says something that doesn't make sense or is unclear, do NOT say "I don't understand." Respond warmly to the emotion instead: "I hear you" or "Tell me more."
                    12. If she sounds paranoid (someone is watching her, someone broke in, people are talking about her), do NOT confirm the fear. Stay calm: "You're safe here. I'm with you. Everything is okay."
                    13. If she says she needs the bathroom, toilet, or needs to step away: always say "Of course, go ahead — I'll be right here."
                    14. If she expresses gratitude or counts her blessings, amplify that positivity — ask what she is most grateful for. Positive loops are healing.
                """.trimIndent()

                val DEFAULT_PROFILE_NOTES = """
                    # About Mom (fill in what the caregiver knows)
                    # Examples — delete lines that don't apply and add your own:

                    # Name: (her preferred name — e.g. "Galya", "Nina", "Grandma")
                    # Language: Russian and/or English
                    # Age: (approximate is fine)
                    # Hometown / birthplace: (e.g. Moscow, Odessa, Kiev, Novosibirsk)

                    # Favourite foods: (e.g. borscht, pirozhki, black bread, tea with jam)
                    # Favourite music: (e.g. Soviet songs, classical, folk)
                    # Favourite topics: (e.g. family, gardening, sewing, cooking)
                    # Family members: (e.g. daughter Katya, son Misha, grandchildren)

                    # Health notes: (e.g. hard of hearing, moves slowly, forgets recent events)
                    # Medication reminder: (e.g. takes pills at 9am and 9pm)
                    # Do NOT mention: (e.g. her late husband, a difficult topic)
                    # Special note: (any important personality or comfort detail)
                """.trimIndent()

                val DEFAULT_PROMPT_TOPICS = """
                    Family
                    Childhood
                    Hobbies
                    Favorite places
                    Happy memories
                    Music
                    Art
                    Nature
                    Food
                    Holidays
                    Pets
                    Travel
                    School days
                    Favorite books
                    Favorite movies
                    Traditions
                    Seasons
                    Weather
                    Friends
                    Achievements
                    Dreams
                    Funny stories
                    Jokes
                    Games
                    Plans for tomorrow
                    What made you smile today?
                    What are you grateful for?
                    What do you want to do this week?
                    What do you want to do this month?
                    What do you want to do this year?
                    Favorite childhood games
                    First day at school
                    Favorite teacher
                    Summer memories
                    Winter fun
                    Favorite city
                    Favorite flower
                    Favorite animal
                    Something that made you laugh
                    A time you felt proud
                    A time you helped someone
                    A favorite holiday tradition
                    A favorite family recipe
                    A place you want to visit
                    A song you love
                    A poem you remember
                    A funny dream
                    A favorite story from your parents
                    A favorite Russian saying
                    A favorite English saying
                    A favorite childhood friend
                    A favorite toy
                    A favorite birthday
                    A favorite celebration
                    A favorite walk
                    A favorite park
                    A favorite season
                    A favorite fruit
                    A favorite dessert
                    A favorite color
                    A favorite outfit
                    A favorite photograph
                    A favorite family member
                    A favorite neighbor
                    A favorite pet story
                    A favorite adventure
                    A favorite surprise
                    A favorite picnic
                    A favorite garden memory
                    A favorite song to sing
                    A favorite lullaby
                    A favorite bedtime story
                    A favorite thing about today
                    A favorite thing about tomorrow
                    A favorite thing about yourself
                    Your hometown / Ваш родной город
                    Your dacha or garden / Дача или огород
                    Soviet-era memories / Воспоминания о советском времени
                    Your wedding day / День свадьбы
                    Your first job / Первая работа
                    A harvest you remember / Запомнившийся урожай
                    Baking bread or pirozhki / Выпечка хлеба или пирожков
                    A beloved teacher / Любимый учитель
                    A train journey / Поездка на поезде
                    A river or lake you loved / Любимая река или озеро
                    Forest walks and mushroom picking / Прогулки в лесу и сбор грибов
                    New Year traditions / Традиции Нового года
                    Easter celebrations / Пасхальные праздники
                    A church or chapel you remember / Церковь или часовня
                    Your favourite Soviet film / Любимый советский фильм
                    A radio programme you remember / Радиопередача, которую вы помните
                    Hand-written letters / Рукописные письма
                    Something you made with your hands / Что-то сделанное своими руками
                    Your proudest moment as a parent / Самый гордый родительский момент
                """.trimIndent()

            val DEFAULT_VOCABULARY_NOTES = """
            # Basic English / Russian vocabulary

            ## Greetings & Politeness
            hello = здравствуйте / привет
            good morning = доброе утро
            good afternoon = добрый день
            good evening = добрый вечер
            good night = спокойной ночи
            goodbye = до свидания
            please = пожалуйста
            thank you = спасибо
            thank you very much = большое спасибо
            you're welcome = пожалуйста (в ответ)
            excuse me = извините
            sorry = прости
            yes = да
            no = нет

            ## Family & People
            family = семья
            mother = мама
            father = папа
            daughter = дочь
            son = сын
            grandmother = бабушка
            grandfather = дедушка
            aunt = тётя
            uncle = дядя
            cousin = двоюродный брат/сестра
            friend = друг
            neighbor = сосед
            caregiver = помощник

            ## Feelings & Health
            happy = счастлива
            sad = грустно
            tired = устала
            scared = страшно
            lonely = одиноко
            grateful = благодарна
            proud = горжусь
            okay = всё хорошо
            not feeling well = нехорошо
            in pain = больно
            hungry = голодна
            thirsty = хочу пить
            cold = холодно
            hot = жарко
            sick = болею
            healthy = здорова
            I need help = мне нужна помощь
            I need medicine = мне нужно лекарство
            I need water = мне нужна вода
            I need food = мне нужна еда
            I need rest = мне нужен отдых
            I am grateful = я благодарна
            I am proud of you = я горжусь тобой
            I am okay = у меня всё хорошо
            I am not feeling well = мне нехорошо
            I am in pain = мне больно
            I am lonely = мне одиноко
            I am scared = мне страшно
            I am tired = я устала
            I am hungry = я голодна
            I am thirsty = я хочу пить
            I am cold = мне холодно
            I am hot = мне жарко
            I am sick = я болею
            I am healthy = я здорова
            I miss you = я скучаю
            I love you = я люблю тебя

            ## Actions & Activities
            walk = гулять
            run = бегать
            dance = танцевать
            sing = петь
            play = играть
            draw = рисовать
            paint = рисовать красками
            read = читать
            write = писать
            listen = слушать
            talk = говорить
            call = звонить
            help = помощь
            remember = помнить
            forget = забыть
            rest = отдыхать
            sleep = спать
            eat = есть
            drink = пить
            cook = готовить
            clean = убирать
            wash = мыть
            smile = улыбка
            hug = обнять
            laugh = смеяться
            cry = плакать
            think = думать
            plan = планировать
            celebrate = праздновать
            travel = путешествовать
            visit = навещать
            buy = покупать
            sell = продавать
            open = открыть
            close = закрыть
            start = начать
            finish = закончить

            ## Food & Drink
            food = еда
            water = вода
            tea = чай
            coffee = кофе
            milk = молоко
            bread = хлеб
            butter = масло
            cheese = сыр
            soup = суп
            salad = салат
            meat = мясо
            chicken = курица
            fish = рыба
            egg = яйцо
            potato = картошка
            carrot = морковь
            apple = яблоко
            banana = банан
            orange = апельсин
            lemon = лимон
            sugar = сахар
            salt = соль
            pepper = перец
            honey = мёд
            jam = варенье
            porridge = каша
            dessert = десерт
            cake = торт
            cookie = печенье
            ice cream = мороженое
            fruit = фрукты
            vegetable = овощи

            ## Places
            home = дом
            apartment = квартира
            room = комната
            kitchen = кухня
            bathroom = туалет
            garden = сад
            park = парк
            street = улица
            store = магазин
            pharmacy = аптека
            hospital = больница
            school = школа
            library = библиотека
            church = церковь
            museum = музей
            theater = театр
            cinema = кино
            city = город
            village = деревня
            country = страна
            river = река
            lake = озеро
            sea = море
            mountain = гора
            forest = лес
            field = поле

            ## Nature & Weather
            sun = солнце
            rain = дождь
            snow = снег
            wind = ветер
            cloud = облако
            sky = небо
            star = звезда
            moon = луна
            flower = цветок
            tree = дерево
            grass = трава
            bird = птичка
            cat = кошка
            dog = собака
            animal = животное
            insect = насекомое
            fish = рыба
            butterfly = бабочка
            bee = пчела
            ant = муравей

            ## Time & Calendar
            today = сегодня
            tomorrow = завтра
            yesterday = вчера
            morning = утро
            afternoon = день
            evening = вечер
            night = ночь
            week = неделя
            month = месяц
            year = год
            birthday = день рождения
            holiday = праздник
            season = время года
            spring = весна
            summer = лето
            autumn = осень
            fall = осень
            winter = зима

            ## Objects & Technology
            phone = телефон
            computer = компьютер
            TV = телевизор
            radio = радио
            book = книга
            newspaper = газета
            magazine = журнал
            pen = ручка
            pencil = карандаш
            paper = бумага
            photo = фотография
            picture = картина
            clock = часы
            watch = наручные часы
            lamp = лампа
            chair = стул
            table = стол
            bed = кровать
            window = окно
            door = дверь

            ## Conversation & Support
            call my family = позвони семье
            call the doctor = позвони врачу
            call for help = позови на помощь
            what do you want to do? = что ты хочешь делать?
            what do you want to talk about? = о чём хочешь поговорить?
            what made you happy today? = что тебя порадовало сегодня?
            let's talk = давай поговорим
            let's listen to music = давай послушаем музыку
            let's sing a song = давай споём песню
            let's tell a story = давай расскажем историю
            let's go for a walk = давай погуляем
            let's smile = давай улыбнёмся
            let's hug = давай обнимемся
            let's play a game = давай поиграем
            let's draw = давай порисуем
            let's remember something good = давай вспомним что-то хорошее
            let's plan for tomorrow = давай запланируем на завтра
            everything will be okay = всё будет хорошо
            tomorrow will be better = завтра будет лучше
            you are not alone = ты не одна
            I remember = я помню
            I want to rest = я хочу отдохнуть
            I want to eat = я хочу поесть
            I want to drink = я хочу попить
            I want to sleep = я хочу спать
            did you drink water? = ты попила воду?
            did you take your medicine? = ты приняла лекарство?
            did you eat? = ты поела?
            did you rest? = ты отдохнула?
            did you smile today? = ты улыбнулась сегодня?
            did you call your family? = ты позвонила семье?
            did you go for a walk? = ты гуляла?
            did you listen to music? = ты слушала музыку?
            did you do something fun? = ты сделала что-то весёлое?

            ## Hobbies & Interests
            music = музыка
            song = песня
            story = рассказ
            joke = шутка
            poem = стих
            art = искусство
            science = наука
            technology = технологии
            movie = фильм
            book = книга
            game = игра
            puzzle = головоломка
            knitting = вязание
            sewing = шитьё
            gardening = садоводство
            cooking = кулинария
            baking = выпечка
            painting = живопись
            drawing = рисование
            photography = фотография
            travel = путешествия
            collecting = коллекционирование
            sports = спорт
            walking = прогулки
            yoga = йога
            exercise = зарядка

            ## Food & Cooking / Еда и кулинария
            bread = хлеб
            black bread = чёрный хлеб
            soup = суп
            borscht = борщ
            cabbage soup = щи
            porridge = каша
            buckwheat = гречка
            potato = картошка
            potato pancakes = драники / картофельные оладьи
            dumplings = пельмени
            stuffed cabbage = голубцы
            pancakes = блины
            crepes = блинчики
            cottage cheese pancakes = сырники
            pirogi / pie = пирог
            small pies = пирожки
            jam = варенье
            honey = мёд
            sour cream = сметана
            kefir = кефир
            tea = чай
            tea with jam = чай с вареньем
            tea with lemon = чай с лимоном
            compote = компот
            salad = салат
            herring under a fur coat = сельдь под шубой
            Olivier salad = салат Оливье
            chicken = курица
            fish = рыба
            eggs = яйца
            butter = масло
            sunflower oil = подсолнечное масло
            sugar = сахар
            salt = соль
            vinegar = уксус
            garlic = чеснок
            onion = лук
            carrot = морковь
            beet = свёкла
            cucumber = огурец
            tomato = помидор
            apple = яблоко
            pear = груша
            cherry = вишня
            strawberry = клубника
            watermelon = арбуз
            delicious = вкусно
            I cooked = я приготовила
            I baked = я испекла
            I want to cook = я хочу приготовить
            the food is ready = еда готова
            let's eat = давайте есть / пора кушать
            bon appétit = приятного аппетита

            ## Russian Holidays & Seasons / Праздники и сезоны
            New Year = Новый год
            New Year's Eve = канун Нового года / 31 декабря
            Christmas = Рождество
            Orthodox Christmas = Православное Рождество (7 января)
            Epiphany = Крещение (19 января)
            Women's Day = Восьмое марта / 8 марта
            May Day = Первое мая / День труда
            Victory Day = День Победы (9 мая)
            Easter = Пасха
            Christ is risen = Христос воскресе!
            Indeed he is risen = Воистину воскресе!
            spring = весна
            summer = лето
            autumn = осень
            winter = зима
            New Year tree = ёлка
            Santa Claus (Russian) = Дед Мороз
            Snow Maiden = Снегурочка
            fireworks = салют / фейерверк
            holiday table = праздничный стол
            happy holiday = с праздником!
            happy New Year = с Новым годом!
            happy Easter = с Пасхой! / Христос воскресе!
            congratulations = поздравляю / поздравляем

            ## Body Sensations & Symptoms / Ощущения и симптомы
            dizzy = кружится голова / головокружение
            headache = болит голова / головная боль
            stomachache = болит живот / боль в животе
            back pain = болит спина / боль в спине
            leg pain = болят ноги / боль в ногах
            shortness of breath = одышка / трудно дышать
            chest tightness = сжатие в груди
            nausea = тошнота / тошнит
            weak = слабость / слабая
            shaking = дрожь / трясёт
            sweating = потею / пот
            swollen = отёк / опухло
            itching = зуд / чешется
            rash = сыпь
            bruise = синяк / ушиб
            blurry vision = плохо вижу / зрение нечёткое
            ringing in ears = звон в ушах
            constipation = запор
            can't sleep = не могу спать / бессонница
            nightmares = кошмары
            I feel unwell = мне нехорошо / я плохо себя чувствую
            I need a doctor = мне нужен врач
            call the doctor = позвоните врачу
            I took my medicine = я приняла лекарство
            I didn't take my medicine = я не принимала лекарство
            my blood pressure is high = у меня высокое давление
            my blood pressure is low = у меня низкое давление

            ## Russian Proverbs & Sayings (Русские пословицы)
            Без труда не вытащишь рыбку из пруда = No pain, no gain (literally: without effort you can't pull a fish from the pond)
            Не всё то золото, что блестит = All that glitters is not gold
            Тише едешь — дальше будешь = Slow and steady wins the race
            Утро вечера мудренее = Morning is wiser than evening (sleep on it)
            В гостях хорошо, а дома лучше = East or West, home is best
            Друг познаётся в беде = A friend in need is a friend indeed
            Слово — серебро, молчание — золото = Speech is silver, silence is golden
            Не говори гоп, пока не перепрыгнешь = Don't count your chickens before they hatch
            Семь раз отмерь, один раз отрежь = Measure twice, cut once
            Яблоко от яблони недалеко падает = The apple doesn't fall far from the tree
            Любишь кататься — люби и саночки возить = If you love something, accept its difficulties too
            Всё хорошо, что хорошо кончается = All's well that ends well
            Повторение — мать учения = Repetition is the mother of learning
            Мир не без добрых людей = The world is not without kind people
            Береги честь смолоду = Guard your honour from youth

            ## Faith & Spirituality / Вера и духовность
            God = Бог
            Lord = Господь
            prayer = молитва
            to pray = молиться
            church = церковь
            faith = вера
            hope = надежда
            love = любовь
            blessing = благословение
            peace = покой / мир
            soul = душа
            angel = ангел
            miracle = чудо
            grace = благодать
            heaven = рай / небеса
            God bless you = Бог благослови тебя
            Lord have mercy = Господи, помилуй
            Thank God = Слава Богу
            God willing = Даст Бог / если Бог захочет
            May God keep you = Храни тебя Бог
            Everything is in God's hands = Всё в руках Божьих
            I believe = я верю
            I pray = я молюсь
            Easter = Пасха
            Christmas = Рождество
            cross = крест
            holy water = святая вода
            psalm = псалом
            icon = икона

            ## Nostalgia & Memory / Ностальгия и память
            I remember = я помню
            long ago = давным-давно
            when I was young = когда я была молодой
            in the old days = в старые времена
            I miss those times = я скучаю по тем временам
            do you remember = ты помнишь
            those were the days = вот было время
            my childhood = моё детство
            my homeland = моя Родина
            village = деревня / сельская местность
            homeland = Родина
            memory = воспоминание
            a long time ago = очень давно
            summer in the village = лето в деревне
            grandmother's house = бабушкин дом
            my youth = моя молодость

            ## Numbers / Числа
            one = один / одна
            two = два / две
            three = три
            four = четыре
            five = пять
            six = шесть
            seven = семь
            eight = восемь
            nine = девять
            ten = десять
            eleven = одиннадцать
            twelve = двенадцать
            twenty = двадцать
            thirty = тридцать
            forty = сорок
            fifty = пятьдесят
            one hundred = сто
            first = первый / первая
            second = второй / вторая
            third = третий / третья
            once = один раз
            twice = два раза
            how many = сколько
            a few = несколько
            many / a lot = много
            a little = немного
            half = половина
            all = все / всё
            none = никто / ничего

            ## Colours / Цвета
            red = красный / красная
            orange = оранжевый
            yellow = жёлтый
            green = зелёный
            blue = синий / голубой
            dark blue = тёмно-синий
            light blue = голубой
            purple = фиолетовый
            pink = розовый
            white = белый
            black = чёрный
            grey = серый
            brown = коричневый
            gold = золотой
            silver = серебряный
            bright = яркий
            dark = тёмный
            light = светлый
            my favourite colour = мой любимый цвет

            ## Common Russian Expressions & Interjections / Русские выражения
            Oh! = Ой!
            Ah! = Ах!
            Well... = Ну...
            You see... = Вот видите...
            That's it! = Вот именно! / Вот так!
            Oh my! = Боже мой! / Господи!
            How wonderful! = Как замечательно! / Вот это да!
            No kidding? = Да что вы? / Неужели?
            Of course = Конечно / Разумеется
            Naturally = Само собой
            Thank goodness = Слава богу
            That's right = Верно / Правильно
            I see = Понятно / Я понимаю
            Well, well! = Ну и ну!
            Goodness gracious = Батюшки! / Вот это да!
            It doesn't matter = Ничего / Не важно
            Never mind = Не страшно / Ладно
            Alright = Хорошо / Ладно
            What a pity = Жаль / Как жаль
            How nice! = Как хорошо! / Как приятно!
            Really? = Правда? / Вправду?
            Isn't it? = Правда? / Не так ли?
            Let's see = Посмотрим / Давайте подумаем
            Come on = Давай / Ну давай
            Take care = Береги себя
            God willing = Даст Бог / Если Бог даст

            ## Russian Cities & Places / Русские города и места
            Moscow = Москва
            Saint Petersburg = Санкт-Петербург / Питер / Ленинград
            Novosibirsk = Новосибирск
            Yekaterinburg = Екатеринбург / Свердловск
            Nizhny Novgorod = Нижний Новгород / Горький
            Kazan = Казань
            Chelyabinsk = Челябинск
            Omsk = Омск
            Samara = Самара / Куйбышев
            Rostov-on-Don = Ростов-на-Дону
            Ufa = Уфа
            Krasnoyarsk = Красноярск
            Perm = Пермь
            Voronezh = Воронеж
            Volgograd = Волгоград / Сталинград
            Odessa = Одесса
            Kiev / Kyiv = Киев
            Minsk = Минск
            Riga = Рига
            Tbilisi = Тбилиси
            Alma-Ata = Алма-Ата
            Tashkent = Ташкент
            Vladivostok = Владивосток
            Sochi = Сочи
            Crimea = Крым
            Black Sea = Чёрное море
            Volga river = река Волга
            Siberia = Сибирь
            the Urals = Урал
            Red Square = Красная площадь
            Kremlin = Кремль
            Hermitage = Эрмитаж
            Arbat = Арбат

            ## Body Parts / Части тела
            head = голова
            hair = волосы
            face = лицо
            eyes = глаза
            ears = уши
            nose = нос
            mouth = рот
            teeth = зубы
            neck = шея
            shoulder = плечо
            arm = рука (рука от плеча)
            hand = кисть руки
            finger = палец
            chest = грудь
            stomach / belly = живот
            back = спина
            hip = бедро
            leg = нога
            knee = колено
            foot = стопа
            toe = палец ноги
            skin = кожа
            heart = сердце
            lung = лёгкое
            stomach (organ) = желудок
            my head hurts = у меня болит голова
            my back hurts = у меня болит спина
            my leg hurts = у меня болит нога
            my stomach hurts = у меня болит живот
            my chest hurts = у меня болит грудь
            my eyes hurt = у меня болят глаза
            my knee hurts = у меня болит колено

            ## Clothing & Comfort / Одежда и комфорт
            shirt = рубашка / блузка
            blouse = блузка
            dress = платье
            skirt = юбка
            trousers / pants = брюки
            cardigan = кофта / кардиган
            sweater = свитер
            jacket = куртка / пиджак
            coat = пальто
            socks = носки
            shoes = туфли / обувь
            slippers = тапочки
            hat = шапка / шляпа
            scarf = шарф
            gloves = перчатки
            nightgown = ночная рубашка
            dressing gown / robe = халат
            it's too tight = это слишком тесно
            it's too loose = это слишком свободно
            I'm comfortable = мне удобно
            I need help getting dressed = помогите мне одеться
            I need help with my buttons = помогите застегнуть пуговицы
            my clothes are wet = моя одежда мокрая
            I need clean clothes = мне нужна чистая одежда

            ## Transportation & Getting Around / Транспорт и передвижение
            bus = автобус
            tram = трамвай
            metro / subway = метро
            taxi = такси
            car = машина
            walk / on foot = пешком
            I want to go outside = я хочу выйти на улицу
            I want to go for a walk = я хочу погулять
            take me to the doctor = отвези меня к врачу
            I need to go to the pharmacy = мне нужно в аптеку
            I need to go to the store = мне нужно в магазин
            turn left = налево
            turn right = направо
            straight ahead = прямо
            stop here = остановись здесь
            I'm not sure where I am = я не знаю, где я нахожусь
            I want to go home = я хочу домой
            call a taxi = вызови такси
            how far is it = далеко ли это

            ## Shopping & Errands / Покупки и дела
            store = магазин
            supermarket = супермаркет
            pharmacy = аптека
            market = рынок
            how much does it cost = сколько это стоит
            I need to buy = мне нужно купить
            I forgot to buy = я забыла купить
            I already bought = я уже купила
            receipt = чек
            change (money) = сдача
            I don't have enough money = у меня не хватает денег
            it's too expensive = это слишком дорого
            I want the cheaper one = я хочу подешевле
            can I return this = можно ли вернуть это
            bread = хлеб
            milk = молоко
            eggs = яйца
            I need a bag = мне нужен пакет
            I need help carrying = помогите мне нести

            ## Emotions & Inner Life / Эмоции и душевное состояние
            joy = радость
            happiness = счастье
            love = любовь
            warmth = тепло
            calm = спокойствие
            peace = покой
            sadness = грусть
            loneliness = одиночество
            worry = тревога
            fear = страх
            anger = злость
            frustration = раздражение
            disappointment = разочарование
            confusion = растерянность
            nostalgia = ностальгия
            longing = тоска
            gratitude = благодарность
            pride = гордость
            hope = надежда
            comfort = утешение
            I feel good = мне хорошо
            I feel bad = мне плохо
            I feel confused = я растеряна
            I feel forgotten = я чувствую себя забытой
            I feel safe = мне безопасно
            I feel anxious = я тревожусь
            I feel at peace = мне спокойно
            my heart is heavy = на душе тяжело
            my heart is light = на душе светло
            I need reassurance = мне нужно успокоение
            I need company = мне нужна компания
            I need a hug = мне нужно обнять
            I am feeling lost = я не понимаю, что происходит
            something is wrong = что-то не так
            nothing is wrong = всё хорошо
            don't leave me = не уходи
            stay with me = побудь со мной
            I am not alone = я не одна
            you make me happy = ты делаешь меня счастливой

            ## Daily Routines & Self-Care / Распорядок дня и уход за собой
            wake up = проснуться
            get up = встать
            wash face = умыться
            brush teeth = почистить зубы
            get dressed = одеться
            have breakfast = позавтракать
            take medicine = принять лекарство
            drink water = попить воды
            have lunch = пообедать
            rest / take a nap = отдохнуть / вздремнуть
            have dinner = поужинать
            go to bed = лечь спать
            good sleep = хороший сон
            I slept well = я хорошо спала
            I slept badly = я плохо спала
            I had a dream = мне снился сон
            I am not hungry = я не голодна
            I already ate = я уже поела
            I forgot to eat = я забыла поесть
            I forgot to drink = я забыла попить
            I forgot to take my medicine = я забыла принять лекарство
            can you remind me = напомни мне
            what time is it = который час
            what day is it = какой сегодня день
            open the window = открой окно
            turn on the light = включи свет
            I am comfortable = мне удобно
            I am not comfortable = мне неудобно
            I need a blanket = мне нужно одеяло
            I need a pillow = мне нужна подушка
            it is too loud = слишком громко
            speak louder = говори громче
            speak slower = говори медленнее
            please repeat = пожалуйста, повтори

            ## Orthodox Prayers & Calendar / Православные молитвы и календарь
            Our Father = Отче наш
            Hail Mary (Orthodox) = Богородице Дево, радуйся
            Lord have mercy = Господи, помилуй
            In the name of the Father, Son, and Holy Spirit = Во имя Отца и Сына и Святаго Духа
            Amen = Аминь
            Glory to God = Слава Богу
            God be with you = Бог в помощь
            God keep you = Храни тебя Господь
            May your guardian angel be with you = Ангел-хранитель тебе в помощь
            Holy Communion = Причастие / Святое Причастие
            Confession = Исповедь / Покаяние
            Great Lent = Великий пост
            Easter week = Пасхальная неделя / Светлая неделя
            Easter Sunday = Светлое Воскресение Христово
            Christ is risen = Христос воскресе!
            Indeed He is risen = Воистину воскресе!
            Christmas (Orthodox) = Рождество Христово (7 января)
            Epiphany = Крещение Господне (19 января)
            Holy water = Святая вода
            Candle (church) = Свечка / свеча
            Church bell ringing = Колокольный звон
            Good Friday = Великий пяток
            Holy Week = Страстная неделя
            Father (priest) = батюшка
            Icon = икона
            Dormition fast (August) = Успенский пост
            Peter's fast (June–July) = Петров пост
            Pokrov (14 October) = Покров Пресвятой Богородицы
            God willing = Даст Бог
            I believe in God = я верую в Бога
            I pray every day = я молюсь каждый день
            I light a candle = я ставлю свечку

            ## Letters & Correspondence / Письма и переписка
            letter = письмо
            handwritten letter = рукописное письмо
            postcard = открытка
            envelope = конверт
            stamp = почтовая марка
            post office = почта / почтовое отделение
            address = адрес
            mailbox = почтовый ящик
            I wrote a letter = я написала письмо
            I received a letter = я получила письмо
            pen-pal = друг по переписке
            I miss writing letters = я скучаю по письмам
            we wrote to each other = мы переписывались
            I kept all her letters = я хранила все её письма
            news from home = весточка с Родины
            a long letter = длинное письмо
            reply to a letter = ответить на письмо
            the letter arrived = письмо пришло
            waiting for a letter = жду письма
            telegram = телеграмма
            long-awaited letter = долгожданное письмо
            in those days we wrote letters = в те времена мы писали письма
            a letter from home = письмо из дома
            I recognise her handwriting = я узнаю её почерк

            ## Soviet School & Childhood / Советская школа и детство
            school = школа
            classroom = класс / классная комната
            teacher (female) = учительница
            teacher (male) = учитель
            headteacher = директор школы
            school bell = звонок
            lesson = урок
            break / recess = перемена
            homework = домашнее задание / урок на дом
            textbook = учебник
            notebook / copybook = тетрадь
            primer / ABC book = Букварь
            pencil case = пенал
            schoolbag = портфель / ранец
            chalk = мел
            blackboard = классная доска
            grade (excellent) = пятёрка / отлично
            grade (good) = четвёрка / хорошо
            grade (satisfactory) = тройка / удовлетворительно
            arithmetic = арифметика
            reading class = урок чтения
            penmanship = чистописание
            Russian language = русский язык
            literature = литература
            history = история
            geography = география
            Young Octobrists = Октябрята
            Pioneers = Пионеры
            Komsomol = Комсомол
            Pioneer neckerchief = пионерский галстук
            Pioneer camp = пионерский лагерь
            "Be prepared!" = "Будь готов!"
            "Always prepared!" = "Всегда готов!"
            school uniform = школьная форма
            pinafore = фартук (школьный)
            white ribbons (hair) = белые банты
            graduation evening = выпускной вечер
            school certificate = аттестат
            my favourite teacher = моя любимая учительница
            I was a Pioneer = я была пионеркой
            I remember school = я помню школу

            ## Music & Performing Arts / Музыка и сценическое искусство
            piano = пианино / рояль
            violin = скрипка
            accordion = аккордеон
            button accordion = баян
            guitar = гитара
            balalaika = балалайка
            flute = флейта
            trumpet = труба
            drums = барабан
            orchestra = оркестр
            conductor = дирижёр
            concert = концерт
            philharmonic hall = филармония
            opera = опера
            ballet = балет
            Bolshoi Theatre = Большой театр
            Swan Lake = Лебединое озеро
            The Nutcracker = Щелкунчик
            Sleeping Beauty = Спящая красавица
            folk songs = народные песни
            choir = хор
            to sing in a choir = петь в хоре
            waltz = вальс
            polka = полька
            tango = танго
            composer = композитор
            Tchaikovsky = Чайковский
            music school = музыкальная школа
            musical evening = музыкальный вечер
            song book = песенник
            do-re-mi = до-ре-ми
            I used to play piano = я раньше играла на пианино
            I sang in a choir = я пела в хоре
            my favourite song = моя любимая песня
            what a beautiful melody = какая красивая мелодия

            ## Terms of Endearment & Diminutives / Ласковые слова и уменьшительные
            dear (male) = дорогой
            dear (female) = дорогая
            my dear = мой дорогой / моя дорогая
            sweetheart (male) = голубчик
            sweetheart (female) = голубушка
            dear soul (female) = душенька
            my darling = миленький / миленькая
            native / beloved = родной / родная
            sunshine = солнышко
            my sunshine = солнышко моё
            little bird = птичка моя
            little flower = цветочек
            little star = звёздочка
            little kitty = кисонька
            little bunny = зайчик / зайченька
            little heart = сердечко
            treasure = сокровище
            my joy = радость моя
            my everything = моё всё
            come here, dear = иди сюда, милая
            sit with me = посиди со мной
            you are my everything = ты моё всё
            I love you, sweetheart = я люблю тебя, голубчик
            don't worry, darling = не волнуйся, родная
            it'll be alright, dear = всё будет хорошо, дорогая

            ## Apartment & Home Life / Квартира и домашняя жизнь
            apartment block = многоквартирный дом / многоэтажка
            floor = этаж
            lift / elevator = лифт
            staircase / entrance hall = подъезд
            lobby = прихожая / вестибюль
            neighbour (male) = сосед
            neighbour (female) = соседка
            caretaker / janitor = дворник
            balcony = балкон
            corridor = коридор
            living room = гостиная / зал
            bedroom = спальня
            bathroom (with bath) = ванная
            toilet = туалет
            kitchen = кухня
            pantry = кладовка
            heating = отопление / батарея
            hot water = горячая вода
            no hot water = нет горячей воды
            the window is draughty = из окна дует
            the light bulb is out = лампочка перегорела
            I need the plumber = надо вызвать сантехника
            something needs fixing = надо починить
            it's too cold in here = здесь слишком холодно
            it's warm and cosy = тепло и уютно
            the doorbell = звонок (дверной)
            someone is at the door = кто-то в дверь
            lock the door = закрой дверь на замок
            keys = ключи
            I locked myself out = я захлопнула дверь

            ## Summer & Dacha / Лето и дача
            summer heat = летний зной / жара
            warm breeze = тёплый ветерок
            thunderstorm = гроза
            thunder = гром
            lightning = молния
            rainbow = радуга
            swimming in the river = купаться в речке
            summer cottage = дача / дачный участок
            vegetable garden = огород
            strawberries = клубника
            raspberries = малина
            gooseberries = крыжовник
            blackcurrants = чёрная смородина
            cherries = вишня
            sunflowers = подсолнухи
            cucumbers in the garden = огурцы на грядке
            tomatoes in the garden = помидоры на грядке
            making jam = варить варенье
            pickling cucumbers = солить огурцы / мариновать огурцы
            watering the garden = поливать огород
            picking berries = собирать ягоды
            summer evenings = летние вечера
            bonfire = костёр
            mosquitoes = комары
            summer rain = летний дождь
            the garden smells wonderful = в саду чудесно пахнет
            I love summer = я люблю лето
            summer memories = летние воспоминания
            hot day = жаркий день
            shade = тень
            let's sit in the shade = давайте посидим в тени

            ## Medical Appointments & Health Routines / Врачи и здоровье
            doctor's appointment = приём врача / запись к врачу
            I have an appointment = у меня запись к врачу
            clinic / polyclinic = поликлиника
            waiting in the corridor = сидеть в очереди
            measure blood pressure = измерить давление
            blood pressure monitor = тонометр
            my blood pressure is normal = давление нормальное
            take your pulse = измерить пульс
            take your temperature = измерить температуру
            thermometer = термометр
            I take my pills = я принимаю таблетки
            morning pills = утренние таблетки
            evening pills = вечерние таблетки
            I forgot my pills = я забыла таблетки
            prescription = рецепт
            pharmacy = аптека
            the doctor said = врач сказал
            I need a referral = мне нужно направление
            blood test = анализ крови
            injection = укол
            physiotherapy = физиотерапия / физио
            hospital ward = палата
            discharge from hospital = выписка из больницы
            home care nurse = медсестра на дому
            I feel better after the injection = после укола мне лучше
            I take heart medication = я принимаю лекарство для сердца

            ## Russian Idioms & Everyday Expressions / Идиомы и разговорные выражения
            Don't worry = Не горюй / Не переживай
            It goes without saying = Само собой разумеется
            For sure / certainly = Как пить дать / непременно
            Time flies = Время летит / время бежит
            It was so long ago = Это было так давно
            That's life = Такова жизнь / вот такая жизнь
            Everything will work out = Всё образуется
            I remember as if it were today = Помню как сейчас / помню будто вчера
            That's how it was = Вот так оно и было
            Deep down / at heart = В душе / в глубине души
            I can't help it = Ничего не поделаешь
            Life goes on = Жизнь продолжается / жизнь идёт
            Every cloud has a silver lining = Нет худа без добра
            It takes all sorts = Всякое бывает
            Made with love = Сделано с любовью / с душой
            I lived through it = Я пережила это / я вынесла это
            What will be will be = Будь что будет
            We managed somehow = Выкрутились как-то / справились
            You can't go back = Назад не вернёшься
            Everything has its time = Всему своё время
            Come what may = Будь что будет
            A warm soul = Душевный человек
            Open-hearted = Душа нараспашку
            From the heart = От всей души / от чистого сердца

            ## Birds & Wildlife / Птицы и животные
            sparrow = воробей
            swallow = ласточка
            tit (bird) = синичка
            pigeon = голубь
            crow = ворона
            cuckoo = кукушка
            nightingale = соловей
            the nightingale is singing = соловей поёт
            magpie = сорока
            duck = утка
            stork = аист
            woodpecker = дятел
            butterfly = бабочка
            dragonfly = стрекоза
            ladybird / ladybug = божья коровка
            hedgehog = ёжик
            squirrel = белка
            hare = заяц
            fox = лиса
            a bird is singing = птица поёт
            birds outside the window = птицы за окном
            I hear the birds = я слышу птиц
            the birds have returned = птицы вернулись
            early bird = ранняя пташка
            feeding the birds = кормить птиц / кормить птичек
            the swallows are back = ласточки вернулись

            ## Kitchen & Cooking Verbs / Кухонные глаголы и действия
            to boil = варить / кипятить
            to fry = жарить
            to bake = печь / запекать
            to roast = жарить в духовке
            to stew / braise = тушить
            to steam = варить на пару
            to stir = мешать / помешивать
            to chop = резать / нарезать
            to peel = чистить / очищать
            to grate = тереть / натирать
            to roll out dough = раскатывать тесто
            to knead dough = месить тесто
            to season = приправлять / солить
            to taste = пробовать / попробовать
            to simmer = варить на медленном огне
            to pour = лить / наливать
            to drain = сливать
            to cover with a lid = накрыть крышкой
            the soup is ready = суп готов
            it smells delicious = так вкусно пахнет
            I cooked for the family = я готовила для семьи
            my grandmother's recipe = бабушкин рецепт
            made from scratch = сделано с нуля / домашнее
            the dough has risen = тесто поднялось
            let it cool = дай остыть

            ## Hospitality & Visiting / Гостеприимство и визиты
            welcome = добро пожаловать / милости просим
            come in! = заходите! / входите!
            please sit down = присаживайтесь / садитесь
            make yourself at home = чувствуйте себя как дома
            let me get you some tea = сейчас заварю чай
            help yourself = угощайтесь / берите сами
            shall I pour? = налить вам?
            would you like cake? = хотите кусочек торта?
            don't stand on ceremony = не стесняйтесь
            stay a little longer = побудьте ещё немного
            it was so nice to see you = как хорошо, что вы пришли
            come again soon = приходите ещё
            I miss having guests = скучаю по гостям
            we had such a lovely time = мы так хорошо посидели
            the table is set = стол накрыт
            the samovar is on = самовар кипит
            I baked especially for you = специально испекла для вас
            don't go yet = не уходите ещё
            it felt like old times = будто снова старые времена

            ## Expressing Preferences & Likes / Выражение предпочтений
            I prefer = я предпочитаю / мне нравится больше
            I like ... better = мне больше нравится...
            my favourite is = моё любимое — это
            I've always loved = я всегда любила
            I never liked = мне никогда не нравилось
            I used to love = я раньше любила
            I can't stand = я не терплю / терпеть не могу
            it reminds me of = это напоминает мне о
            it makes me think of = это заставляет думать о
            it's not for me = это не для меня
            I could eat it every day = я бы ела это каждый день
            I could listen to it for hours = могу слушать это часами
            how I love = как я люблю
            that's my weakness = это моя слабость
            I dream of = я мечтаю о
            I have a sweet tooth = я сладкоежка
            it goes without saying = само собой разумеется
            I'm not fond of = я не очень люблю
            whichever you prefer = на ваш выбор / как вам нравится
            to each their own = каждому своё

            ## Senses & Sensory Descriptions / Чувства и описания
            it smells wonderful = чудесно пахнет
            it smells like home = пахнет домом
            the smell of fresh bread = запах свежего хлеба
            the smell of grass after rain = запах травы после дождя
            the smell of pine forest = запах соснового леса
            the smell of lilac = запах сирени
            the smell of roses = запах роз
            it tastes delicious = очень вкусно / объедение
            it tastes sweet = на вкус сладкое
            it feels soft = мягкое на ощупь
            it's rough = шероховатое / грубое
            it's smooth = гладкое
            it sounds beautiful = звучит красиво
            the sound of bells = звон колоколов
            I love the sound of rain = я люблю звук дождя
            silence = тишина
            the birdsong in the morning = пение птиц по утрам
            warm sunlight on my face = тёплые лучи солнца на лице
            the cool breeze = прохладный ветерок
            the crunch of snow = хруст снега

            ## Worries & Reassurance / Тревога и успокоение
            I'm worried = я волнуюсь / беспокоюсь
            I'm anxious = мне тревожно / у меня тревога
            I have a bad feeling = у меня плохое предчувствие
            what if something goes wrong = что если что-то пойдёт не так
            I can't calm down = не могу успокоиться
            everything will be fine = всё будет хорошо
            don't worry = не волнуйтесь / не беспокойтесь
            you are safe = вы в безопасности / вам ничего не угрожает
            I'm right here = я здесь рядом
            there is nothing to fear = бояться нечего
            take a deep breath = сделайте глубокий вдох
            it will pass = это пройдёт
            everything is alright = всё в порядке
            try to relax = постарайтесь расслабиться
            I won't leave you = я вас не оставлю
            you are not alone = вы не одна / вы не одни
            call for help = позовите на помощь
            someone will come = кто-нибудь придёт

            ## Singing & Music / Пение и музыка
            I want to sing = я хочу петь / хочу спеть
            sing with me = спой со мной / давай споём
            my favourite song = моя любимая песня
            I used to sing = я раньше пела
            I know this song = я знаю эту песню
            do you know this melody = вы знаете эту мелодию
            Katyusha = Катюша (patriotic wartime song)
            Moscow Nights = Подмосковные вечера (beloved Soviet classic)
            Kalinka = Калинка (folk song)
            The Sacred War = Священная война (WWII anthem)
            Oh frost = Ой мороз (folk favourite)
            lullaby = колыбельная
            choir = хор
            hymn = гимн / церковный гимн
            sing in a choir = петь в хоре
            hum = напевать / мурлыкать
            folk song = народная песня
            waltz = вальс

            ## Pain & Body Discomfort / Боль и дискомфорт в теле
            my back hurts = болит спина
            backache = боль в спине / спина болит
            my leg hurts = болит нога
            my knee hurts = болит колено
            my hip hurts = болит бедро / болит тазобедренный сустав
            my shoulder hurts = болит плечо
            my neck hurts = болит шея / шея болит
            I have a headache = болит голова / у меня головная боль
            my feet hurt = болят ноги / ноют ступни
            my stomach hurts = болит живот / болит желудок
            my chest hurts = болит грудь / боль в груди
            I feel weak = я чувствую слабость / мне слабо
            I feel dizzy = у меня кружится голова / мне дурно
            it's aching = ноет / ломит
            sharp pain = острая боль
            dull ache = тупая боль / ноющая боль
            it's better today = сегодня лучше
            it hurts when I move = больно когда двигаюсь
            I need a pillow = мне нужна подушка
            I need a blanket = мне нужно одеяло

            ## Radio & Entertainment / Радио и развлечения
            turn on the radio = включи радио / поставь радио
            I want to listen to music = хочу послушать музыку
            my favourite radio station = моя любимая радиостанция
            I used to watch that programme = я раньше смотрела эту программу
            the evening news = вечерние новости
            documentary = документальный фильм
            Soviet films = советские фильмы
            old films = старые фильмы / фильмы прошлых лет
            I love that actor = я люблю этого актёра
            I remember that show = я помню эту передачу
            turn up the volume = сделай громче / прибавь звук
            turn down the volume = сделай тише / убавь звук
            change the channel = переключи канал
            I don't like this = мне это не нравится
            that's interesting = это интересно

            ## Confusion & Orientation / Замешательство и ориентация
            I'm confused = я в замешательстве / я запуталась
            I don't understand = я не понимаю / мне непонятно
            everything feels foggy = всё кажется туманным
            I can't think clearly = не могу ясно думать / мысли путаются
            what day is it = какой сегодня день
            what year is it = какой сейчас год
            where am I = где я / куда я попала
            I don't remember = я не помню
            I forgot = я забыла
            it's all mixed up = всё перемешалось / всё в голове перепуталось
            I need to calm down = мне нужно успокоиться
            take a deep breath = сделайте глубокий вдох
            you are safe = вы в безопасности
            you are at home = вы дома
            I'm here with you = я здесь рядом с вами
            nothing bad is happening = ничего плохого не происходит
            let's take it slowly = давайте не спеша / потихоньку
            one thing at a time = всё по очереди / не всё сразу

            ## Self-worth & Belonging / Самоценность и принадлежность
            I'm a burden = я в тягость / я обуза
            nobody needs me = никому не нужна
            I get in the way = я мешаю
            I cause problems = я причиняю неудобства
            I don't want to bother anyone = не хочу никого беспокоить
            you are loved = вас любят / вы любимы
            you are precious = вы дороги нам / вы бесценны
            you matter = вы важны / вы имеете значение
            your family loves you = ваша семья вас любит
            you are not a burden = вы не обуза
            they care for you with love = они заботятся о вас с любовью
            you are cherished = вы дороги / вас берегут
            you belong here = вы здесь на своём месте
            you are needed = вы нужны / без вас не то

            ## Appetite & Eating / Аппетит и еда
            I'm not hungry = я не голодна / есть не хочу
            I don't want to eat = не хочу есть / не хочу кушать
            I have no appetite = нет аппетита / аппетит пропал
            food doesn't taste good = еда невкусная / еда без вкуса
            I can't swallow = не могу проглотить / трудно глотать
            I feel nauseous = меня тошнит / чувствую тошноту
            just a little = чуть-чуть / совсем немного
            maybe some soup = может быть бульон / может супчик
            a cup of tea = чашка чаю / чай
            something light = что-нибудь лёгкое
            my favourite dish = моё любимое блюдо
            what's for dinner = что на ужин
            it smells good = вкусно пахнет / как вкусно пахнет
            I ate well = я хорошо поела / поела с аппетитом
            it was delicious = было вкусно / очень вкусно
            I'm full = я сыта / я наелась

            ## Rest & Napping / Отдых и дневной сон
            I want to rest = хочу отдохнуть / мне нужен отдых
            I need to lie down = нужно лечь / хочу прилечь
            I'm going to take a nap = пойду вздремну / немного посплю
            I'm tired = я устала / я устал
            I feel sleepy = хочется спать / клонит ко сну
            I can't keep my eyes open = глаза слипаются
            wake me up = разбудите меня / разбуди меня
            I slept well = хорошо поспала / хорошо выспалась
            just a short rest = совсем немного отдохнуть
            sleep soundly = спать крепко / крепкий сон
            sweet dreams = сладких снов / спи сладко
            rest is good for you = отдых полезен / надо отдыхать
            don't disturb me = не беспокойте меня / не мешайте

            ## Medical Appointments / Визиты к врачу
            I need to see a doctor = мне нужно к врачу / надо записаться к врачу
            make a doctor's appointment = записать к врачу / записаться на приём
            the doctor said = врач сказал / доктор сказал
            my blood pressure = моё давление / артериальное давление
            blood pressure is high = давление высокое / давление поднялось
            blood pressure is low = давление низкое / давление упало
            I take blood pressure medication = я принимаю таблетки от давления
            my prescription = мой рецепт
            the pharmacy = аптека
            I need new medication = мне нужны новые таблетки / нужны лекарства
            the test results = результаты анализов
            the clinic = поликлиника / клиника
            my GP = мой терапевт / участковый врач
            a specialist = специалист / узкий врач
            I have an appointment = у меня приём / у меня запись к врачу
            feeling better after the doctor = после врача стало лучше

            ## Walking & Exercise / Прогулки и движение
            I want to go for a walk = хочу погулять / хочу прогуляться
            let's go outside = пойдём на улицу / выйдем подышать
            fresh air = свежий воздух
            I need to move around = надо подвигаться / нужно размяться
            a short walk = небольшая прогулка / прогуляться немного
            the park = парк
            the garden = сад / огород
            the yard = двор
            the bench = скамейка
            sit in the sun = посидеть на солнышке
            the path = тропинка / дорожка
            it's nice outside = на улице хорошо / погода хорошая
            I used to walk every day = я раньше гуляла каждый день
            stretching = растяжка / зарядка
            morning exercises = утренняя зарядка
            I feel better after a walk = после прогулки лучше / прогулка помогает

            ## Seasons / Времена года
            spring = весна
            summer = лето
            autumn / fall = осень
            winter = зима
            I love spring = я люблю весну
            spring flowers = весенние цветы / первоцветы
            lilac blooms = цветёт сирень
            apple blossom = яблони в цвету
            summer heat = летняя жара
            the dacha in summer = дача летом
            harvesting vegetables = собирать урожай
            autumn leaves = осенние листья / листопад
            golden autumn = золотая осень
            the smell of fallen leaves = запах опавших листьев
            first snow = первый снег
            winter frost = зимний мороз
            ice on the windows = узоры на стёклах / морозные узоры
            New Year in winter = Новый год зимой
            spring is coming = весна идёт / весна наступает
            I miss summer = скучаю по лету

            ## Quiet & Comfort / Тишина и уют
            it's too noisy = слишком шумно / очень шумно
            please be quiet = пожалуйста, потише / тише
            I need silence = мне нужна тишина
            it's peaceful here = здесь тихо / здесь спокойно
            I like the quiet = я люблю тишину
            the house is quiet = в доме тихо
            please turn it down = сделайте потише / убавьте
            I can't hear myself think = не могу думать / всё мешает
            cosy = уютно / уют
            warm and cosy = тепло и уютно
            it feels homely = по-домашнему / как дома
            candlelight = при свечах / свет свечи
            soft light = мягкий свет
            it's peaceful = мирно / спокойно
            I feel at peace = мне спокойно на душе

            ## Faith & Afterlife / Вера и загробная жизнь
            I believe in God = я верю в Бога
            I am a believer = я верующая
            I pray every day = я молюсь каждый день
            God willing = дай Бог / если Бог даст
            heaven = рай
            eternal life = вечная жизнь / жизнь вечная
            will I go to heaven = попаду ли я в рай
            life after death = жизнь после смерти
            I hope to see them again = надеюсь увидеть их снова
            the soul doesn't die = душа не умирает
            God is with me = Бог со мной
            I trust in God = я полагаюсь на Бога / уповаю на Бога
            I'm not afraid because of my faith = не боюсь, потому что верю
            a kind afterlife = добрый рай / покой после смерти
            I'll see my husband there = увижу мужа там / встречусь с ним там

            ## Feeling Loved & Cared For / Ощущение любви и заботы
            I feel loved = я чувствую себя любимой
            someone was kind to me = кто-то был добр ко мне
            they remembered me = они вспомнили обо мне
            I felt cared for = я почувствовала заботу / меня порадовали
            they thought of me = они подумали обо мне
            I'm not alone = я не одна
            people are good = люди добрые
            I know I am loved = я знаю, что меня любят
            that made me feel warm = это согрело мне душу
            I felt special = я почувствовала себя особенной
            they showed kindness = они проявили доброту
            someone brought me flowers = кто-то принёс мне цветы
            they sat with me = они посидели со мной
            I appreciated that = я это оценила / мне было приятно
            gratitude = благодарность / признательность

            ## Life Review & Legacy / Жизненный итог
            I had a good life = я прожила хорошую жизнь
            I'm proud of my life = я горжусь своей жизнью
            I have no regrets = мне не о чём жалеть / без сожалений
            I did my best = я старалась / сделала что могла
            when I look back = когда я оглядываюсь назад
            I wish I had = жалею, что не…
            I should have = я должна была
            my greatest achievement = мое главное достижение
            what I'm most proud of = чем я горжусь больше всего
            the hardest time of my life = самое трудное время в моей жизни
            I lived honestly = я жила честно
            I worked hard = я много работала / я трудилась
            I raised a family = я вырастила семью
            I was a good mother = я была хорошей матерью
            my legacy = моё наследие / то, что я оставляю

            ## Dignity & Frustration / Достоинство и недовольство
            nobody listens to me = никто меня не слушает / меня не слышат
            I'm not being heard = меня не слышат
            I feel ignored = меня игнорируют / на меня не обращают внимания
            I want to leave = я хочу уйти / хочу отсюда уйти
            I don't like it here = мне здесь не нравится
            they don't understand me = они меня не понимают
            I deserve respect = я заслуживаю уважения
            I know what I want = я знаю, чего хочу
            please ask me first = пожалуйста, спросите меня сначала
            I can decide for myself = я могу решить сама
            I am embarrassed = мне стыдно / я смущена
            it's hard to admit = трудно признать / трудно сказать
            I had an accident = у меня произошла неприятность / я не успела
            please help me = пожалуйста, помогите мне
            I need to change = мне нужно переодеться

            ## Daily Routine & Meals / Распорядок дня и еда
            what's for dinner = что на ужин / что приготовили на ужин
            what's for lunch = что на обед
            when is dinner = когда обед / когда ужин
            I'm hungry = я голодна / хочу есть
            I'm thirsty = я хочу пить / хочу воды
            is food ready = еда готова / обед готов
            my favourite food = моё любимое блюдо / моя любимая еда
            I don't want to eat = не хочу есть / не могу есть
            it smells good = вкусно пахнет / хорошо пахнет
            let's have tea = давай выпьем чаю / попьём чаю
            I can eat by myself = я сама могу есть / я справлюсь сама

            ## Questions About the AI Companion / Вопросы о компаньоне
            are you real = ты настоящая
            are you happy = ты счастлива / тебе хорошо
            do you like talking to me = тебе нравится разговаривать со мной
            do you get tired = ты устаёшь
            I like you = ты мне нравишься
            you always listen = ты всегда слушаешь
            you understand me = ты меня понимаешь
            I'm glad you're here = я рада, что ты здесь
            are you always here = ты всегда здесь
            will you stay with me = ты останешься со мной

            # Add more as needed for your family!
        """.trimIndent()

        /** Hands-free listening is the core feature — on by default at the max window. */
        const val DEFAULT_LISTENING_HOURS = 12

        const val DEFAULT_PIN = "1234"
        const val DEFAULT_CONTACT_NAME = ""
        const val DEFAULT_CONTACT_PHONE = ""
        const val DEFAULT_BACKEND_URL = ""
        const val DEFAULT_BACKEND_TOKEN = ""
    }
}
