from selenium import webdriver
import pyautogui, time, clipboard
import io
import os

file_path = os.getcwd() + "\\servers_payments.txt"
driver_path = os.getcwd() + "\\phantomjs.exe"
websites = [""]


def main():
    try:
        open(file_path, 'w+').close()
        for i in range(0, websites.__len__()):
            driver = webdriver.PhantomJS(driver_path)
            driver.set_window_size(1120, 550)
            try:
                driver.get(websites[i])
                time.sleep(5)
                html = driver.page_source
                time.sleep(1)
                driver.quit()
                payments = html[html.find("<div class=\"payment-list\">"):html.find("</div>\n\n\t<div class=\"modal\" data-id=\"paymodal\">")]
            except Exception:
                payments = "#scan_error"
            with io.open(file_path, "a", encoding="utf-8") as file:
                if i != websites.__len__() - 1:
                    file.write(payments + "\n")
                    file.close()
                else:
                    file.write(payments)
    except Exception:
        driver.quit()
        exit(1)


def get_mustery():
    #time.sleep(5)
    pyautogui.moveTo(50, 210, duration=0.5)
    pyautogui.click()
    pyautogui.hotkey('F5')
    time.sleep(15)
    pyautogui.rightClick()
    pyautogui.moveTo(150, 505, duration=1)
    pyautogui.click()
    pyautogui.moveTo(350, 505, duration=1)
    pyautogui.click()
    html = clipboard.paste()
    return html[html.find("<div class=\"payment-list\">"): html.find("</div>\n\n\t<div class=\"modal\" data-id=\"paymodal\">")]


if __name__ == '__main__':
    main()