# -*- coding: utf-8 -*-
import os
import sys

def convert_class_to_cpp(class_file_path):
    """
    读取一个 .class 文件并将其转换为 C++ jbyte[] 数组。
    """
    if not os.path.exists(class_file_path):
        print(f"\n错误：文件未找到: {class_file_path}")
        print("请确保你提供了 .class 文件的正确路径。")
        return

    try:
        with open(class_file_path, 'rb') as f:
            class_bytes = f.read()

        byte_count = len(class_bytes)
        if byte_count == 0:
            print("错误：文件为空。")
            return

        print(f"\n正在转换 {os.path.basename(class_file_path)} ({byte_count} 字节)...")

        # 开始构建 C++ 数组字符串
        cpp_lines = []
        cpp_lines.append(f"// C++ 数组由 converter.py 自动生成")
        cpp_lines.append(f"// 原始文件: {os.path.basename(class_file_path)}")
        cpp_lines.append(f"// 总大小: {byte_count} 字节")
        cpp_lines.append(f"jbyte loader_class_data[{byte_count}] = {{")
        
        line_buffer = []
        for i, byte in enumerate(class_bytes):
            # jbyte 是 signed char (-128 到 127)
            # Python 的 bytes 是 unsigned (0 到 255)
            # 我们需要转换它
            signed_val = byte if byte <= 127 else byte - 256
            
            line_buffer.append(str(signed_val))

            # 每 20 个字节换一行，或者到达文件末尾
            is_last_byte = (i + 1) == byte_count
            if (i + 1) % 20 == 0 or is_last_byte:
                cpp_lines.append("    " + ", ".join(line_buffer) + ("" if is_last_byte else ","))
                line_buffer = []
        
        
        cpp_lines.append("};")

        # 将所有行合并为一个字符串
        final_code = "\n".join(cpp_lines)
        
        # 将结果保存到文件，因为打印到控制台可能会有大小限制
        output_filename = "cpp_array.txt"
        with open(output_filename, 'w', encoding='utf-8') as out_f:
            out_f.write(final_code)
        
        print("\n--------------------------------------------------")
        print(f"转换成功！ C++ 数组已保存到: {output_filename}")
        print("--------------------------------------------------")
        print(f"\n下一步: ")
        print(f"1. 打开 {output_filename} 文件。")
        print(f"2. 复制里面的所有内容。")
        print(f"3. 粘贴到你的 C++ 文件中 (YaoMaoFucker.cpp)，完全替换旧的 'loader_class_data' 数组。")

    except Exception as e:
        print(f"发生错误: {e}")

if __name__ == "__main__":
    print("--- .class 文件转 C++ jbyte 数组工具 ---")
    print("本工具用于将已编译的Java .class 文件转换为 C++ 字节数组")
    
    if sys.version_info[0] < 3:
        print("错误：此脚本需要 Python 3.x 运行。")
        sys.exit(1)
        
    try:
        class_path = input("请输入你编译后的 .class 文件的完整路径: ").strip().strip('"').strip("'")
        
        convert_class_to_cpp(class_path)
        
    except KeyboardInterrupt:
        print("\n操作已取消。")
    except Exception as e:
        print(f"\n发生未知错误: {e}")
        
    if os.name == 'nt':
        input("\n按 Enter 键退出...")