#!/usr/bin/env python3
"""Демо-календарь для скриншотов Play: локальный календарь + события вокруг «сейчас».

python3 demo_calendar.py en|ru|de|es|fr|hi|clear
"""
import datetime as dt
import os
import shlex
import subprocess
import sys

ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
SERIAL = "emulator-5554"
ACC = "CaliindaDemo"
TZ = "Asia/Yekaterinburg"


def sh(cmd):
    return subprocess.run([ADB, "-s", SERIAL, "shell", cmd], capture_output=True, text=True).stdout


def insert(uri, **vals):
    binds = []
    for k, v in vals.items():
        t = "i" if isinstance(v, int) and abs(v) < 2**31 else "l" if isinstance(v, int) else "s"
        binds.append(f"--bind {k}:{t}:{shlex.quote(str(v))}")
    sh(f"content insert --uri {shlex.quote(uri)} " + " ".join(binds))


def device_now():
    # Время берём с эмулятора: часы там можно сдвинуть (`cmd alarm set-time`) на удобный час.
    return dt.datetime.fromtimestamp(int(sh("date +%s").strip())).astimezone()


def clear():
    sh(f"content delete --uri 'content://com.android.calendar/calendars?caller_is_syncadapter=true"
       f"&account_name={ACC}&account_type=LOCAL' --where \"account_name='{ACC}'\"")


T = {
    "en": dict(cal="Personal", breakfast="🍳 Breakfast", gym="🏋️‍♀️ Going to the Gym",
               lunch="🥪 Lunch Break", project="💻 Working on a Project", coffee="☕ Coffee with Anna",
               dinner="🍽️ Dinner", movie="🎬 Movie Night", bday="🎂 Mom's birthday",
               trip="✈️ Trip to Lisbon", course="📚 Spanish course", moving="📦 Moving",
               vacation="🌴 Vacation", walk="🚶 Walking", call="📞 Phone Call", party="🎉 Party",
               place="Café Lumière"),
    "ru": dict(cal="Личный", breakfast="🍳 Завтрак", gym="🏋️‍♀️ Поход в спортзал",
               lunch="🥪 Обед", project="💻 Работа над проектом", coffee="☕ Кофе с Аней",
               dinner="🍽️ Ужин", movie="🎬 Вечер кино", bday="🎂 День рождения мамы",
               trip="✈️ Поездка в Лиссабон", course="📚 Курс испанского", moving="📦 Переезд",
               vacation="🌴 Отпуск", walk="🚶 Прогулка", call="📞 Телефонный звонок",
               party="🎉 Вечеринка", place="Кафе «Люмьер»"),
    "de": dict(cal="Privat", breakfast="🍳 Frühstück", gym="🏋️‍♀️ Ab ins Fitnessstudio",
               lunch="🥪 Mittagspause", project="💻 Arbeit am Projekt", coffee="☕ Kaffee mit Anna",
               dinner="🍽️ Abendessen", movie="🎬 Filmabend", bday="🎂 Mamas Geburtstag",
               trip="✈️ Reise nach Lissabon", course="📚 Spanischkurs", moving="📦 Umzug",
               vacation="🌴 Urlaub", walk="🚶 Spaziergang", call="📞 Telefonat", party="🎉 Party",
               place="Café Lumière"),
    "es": dict(cal="Personal", breakfast="🍳 Desayuno", gym="🏋️‍♀️ Al gimnasio",
               lunch="🥪 Almuerzo", project="💻 Trabajo en el proyecto", coffee="☕ Café con Ana",
               dinner="🍽️ Cena", movie="🎬 Noche de cine", bday="🎂 Cumpleaños de mamá",
               trip="✈️ Viaje a Lisboa", course="📚 Curso de inglés", moving="📦 Mudanza",
               vacation="🌴 Vacaciones", walk="🚶 Paseo", call="📞 Llamada", party="🎉 Fiesta",
               place="Café Lumière"),
    "fr": dict(cal="Personnel", breakfast="🍳 Petit-déjeuner", gym="🏋️‍♀️ Séance de sport",
               lunch="🥪 Pause déjeuner", project="💻 Travail sur le projet", coffee="☕ Café avec Anna",
               dinner="🍽️ Dîner", movie="🎬 Soirée ciné", bday="🎂 Anniversaire de maman",
               trip="✈️ Voyage à Lisbonne", course="📚 Cours d'espagnol", moving="📦 Déménagement",
               vacation="🌴 Vacances", walk="🚶 Balade", call="📞 Appel", party="🎉 Fête",
               place="Café Lumière"),
    "hi": dict(cal="निजी", breakfast="🍳 नाश्ता", gym="🏋️‍♀️ जिम जाना",
               lunch="🥪 लंच ब्रेक", project="💻 प्रोजेक्ट पर काम", coffee="☕ अनन्या के साथ कॉफ़ी",
               dinner="🍽️ डिनर", movie="🎬 मूवी नाइट", bday="🎂 माँ का जन्मदिन",
               trip="✈️ गोवा की यात्रा", course="📚 स्पेनिश कोर्स", moving="📦 घर बदलना",
               vacation="🌴 छुट्टियाँ", walk="🚶 सैर", call="📞 फ़ोन कॉल", party="🎉 पार्टी",
               place="Café Lumière"),
}


def week_events(ev, today, tz, t):
    load = [3, 1, 4, 2, 5, 2, 3]  # сколько событий по дням недели
    titles = [t["walk"], t["call"], t["lunch"], t["gym"], t["dinner"]]
    for back in range(1, 7):
        d = today - dt.timedelta(days=back)
        base = dt.datetime.combine(d, dt.time(9), tz)
        for i in range(load[d.weekday()]):
            ev(titles[i], base + dt.timedelta(hours=2 * i), 60)


def main():
    lang = sys.argv[1]
    if lang.startswith("week-"):  # только неделя — не трогая остальное (виджеты держат id событий)
        return week_only(T[lang[5:]])
    clear()
    if lang == "clear":
        return
    t = T[lang]
    insert(f"content://com.android.calendar/calendars?caller_is_syncadapter=true"
           f"&account_name={ACC}&account_type=LOCAL",
           account_name=ACC, account_type="LOCAL", name=ACC, calendar_displayName=t["cal"],
           calendar_color=0xFFFFC94D - 2**32, calendar_access_level=700, ownerAccount=ACC,
           visible=1, sync_events=1, calendar_timezone=TZ)
    cal_id = sh(f"content query --uri content://com.android.calendar/calendars --projection _id "
                f"--where \"account_name='{ACC}'\"").split("_id=")[1].split()[0]

    now = device_now()
    n = now.replace(second=0, microsecond=0)
    n = n - dt.timedelta(minutes=n.minute % 15)
    today = n.date()

    def ev(title, start, minutes, **extra):
        insert("content://com.android.calendar/events", calendar_id=int(cal_id), title=title,
               dtstart=int(start.timestamp() * 1000),
               dtend=int((start + dt.timedelta(minutes=minutes)).timestamp() * 1000),
               eventTimezone=TZ, **extra)

    def allday(title, first, days):
        s = dt.datetime.combine(first, dt.time(), dt.timezone.utc)
        insert("content://com.android.calendar/events", calendar_id=int(cal_id), title=title,
               allDay=1, dtstart=int(s.timestamp() * 1000),
               dtend=int((s + dt.timedelta(days=days)).timestamp() * 1000), eventTimezone="UTC")

    m = lambda k: n + dt.timedelta(minutes=k)
    # Сегодня: «сейчас» — работа над проектом, прошла примерно половина.
    ev(t["breakfast"], m(-390), 30)
    ev(t["gym"], m(-315), 75)
    ev(t["lunch"], m(-180), 45)
    ev(t["project"], m(-75), 135)
    ev(t["coffee"], m(75), 30, eventLocation=t["place"])
    ev(t["dinner"], m(210), 90)
    ev(t["movie"], m(330), 120)
    allday(t["bday"], today, 1)
    # Долгие: поездка — «день 3 из 7», курс на месяц, переезд и отпуск впереди.
    allday(t["trip"], today - dt.timedelta(days=2), 7)
    allday(t["course"], today - dt.timedelta(days=9), 30)
    allday(t["moving"], today + dt.timedelta(days=6), 3)
    allday(t["vacation"], today + dt.timedelta(days=12), 14)
    # Завтра — чтобы при листании был не пустой день.
    tm = dt.datetime.combine(today + dt.timedelta(days=1), dt.time(), now.tzinfo)
    ev(t["breakfast"], tm.replace(hour=9), 45)
    ev(t["walk"], tm.replace(hour=11), 90)
    ev(t["call"], tm.replace(hour=14), 30)
    ev(t["party"], tm.replace(hour=19), 180)
    # Прошедшие дни недели — разная загрузка, чтобы виджет «Неделя» не был пустым.
    week_events(ev, today, now.tzinfo, t)
    print("calendar", cal_id, "now", n.strftime("%H:%M"))


def week_only(t):
    cal_id = sh(f"content query --uri content://com.android.calendar/calendars --projection _id "
                f"--where \"account_name='{ACC}'\"").split("_id=")[1].split()[0]
    now = device_now()

    def ev(title, start, minutes):
        insert("content://com.android.calendar/events", calendar_id=int(cal_id), title=title,
               dtstart=int(start.timestamp() * 1000),
               dtend=int((start + dt.timedelta(minutes=minutes)).timestamp() * 1000), eventTimezone=TZ)
    week_events(ev, now.date(), now.tzinfo, t)


main()
