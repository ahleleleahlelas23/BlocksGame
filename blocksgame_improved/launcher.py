import subprocess
import os
import sys

def main():
    # Определяем путь к JRE
    jre_path = os.path.join(os.path.dirname(sys.executable) if getattr(sys, 'frozen', False) else os.path.dirname(__file__), 'jre')
    java_exe = os.path.join(jre_path, 'bin', 'java.exe') if os.name == 'nt' else os.path.join(jre_path, 'bin', 'java')
    
    # Путь к JAR файлу
    jar_path = os.path.join(os.path.dirname(sys.executable) if getattr(sys, 'frozen', False) else os.path.dirname(__file__), 'app', 'blocksgame_0.2.2_2_beta.jar')
    
    if not os.path.exists(java_exe):
        print(f"Java not found at {java_exe}")
        input("Press Enter to exit...")
        return
    
    if not os.path.exists(jar_path):
        print(f"Game JAR not found at {jar_path}")
        input("Press Enter to exit...")
        return
    
    # Запускаем игру
    subprocess.run([java_exe, '-jar', jar_path])

if __name__ == '__main__':
    main()
