//===============================================================================================//
// 版权所有 (c) 2012, Stephen Fewer of Harmony Security (www.harmonysecurity.com)
// 保留所有权利。
// 
// 在满足以下条件的情况下，允许以源代码和二进制形式重新分发和使用，无论是否进行修改：
// 
//     * 源代码的重新分发必须保留上述版权声明、此条件列表和以下免责声明。
// 
//     * 二进制形式的重新分发必须在随分发提供的文档和/或其他材料中
//       重现上述版权声明、此条件列表和以下免责声明。
// 
//     * 未经事先书面许可，不得使用 Harmony Security 的名称或其贡献者的名称
//       来认可或推广从此软件衍生的产品。
// 
// 此软件由版权持有者和贡献者"按原样"提供，不承担任何明示或
// 暗示的保证，包括但不限于对适销性和特定用途适用性的暗示保证。
// 在任何情况下，版权所有者或贡献者均不对任何直接、间接、偶然、特殊、惩戒性或
// 后果性损害（包括但不限于替代商品或服务的采购；使用、数据或利润的损失；
// 或业务中断）承担责任，无论是由于任何责任理论、合同、严格责任或侵权
//（包括疏忽或其他）引起的，即使已被告知此类损害的可能性。
//===============================================================================================//
#include "ReflectiveLoader.h"
//===============================================================================================//
// 我们的加载器将把这个设置为一个伪正确的 HINSTANCE/HMODULE 值
HINSTANCE hAppInstance = NULL;
//===============================================================================================//
#pragma intrinsic( _ReturnAddress )
// 这个函数不能被编译器内联，否则我们将无法获得期望的地址。理想情况下
// 这段代码应该使用 /O2 和 /Ob1 开关编译。如果我们能在这种情况下利用
// RIP 相对寻址就更好了，但我认为我们无法使用可用的编译器内联函数来实现
//（而且在 x64 下没有内联汇编可用）。
__declspec(noinline) ULONG_PTR caller( VOID ) { return (ULONG_PTR)_ReturnAddress(); }
//===============================================================================================//

// 注意 1: 如果你想要自己的 DllMain，请定义 REFLECTIVEDLLINJECTION_CUSTOM_DLLMAIN，
//         否则将使用此文件末尾的 DllMain。

// 注意 2: 如果你通过 LoadRemoteLibraryR 注入 DLL，请定义 REFLECTIVEDLLINJECTION_VIA_LOADREMOTELIBRARYR，
//         否则假定你通过存根调用 ReflectiveLoader。

// 这是我们的位置无关反射式 DLL 加载器/注入器
#ifdef REFLECTIVEDLLINJECTION_VIA_LOADREMOTELIBRARYR
DLLEXPORT ULONG_PTR WINAPI ReflectiveLoader( LPVOID lpParameter )
#else
DLLEXPORT ULONG_PTR WINAPI ReflectiveLoader( VOID )
#endif
{
	// 我们需要的函数
	LOADLIBRARYA pLoadLibraryA     = NULL;
	GETPROCADDRESS pGetProcAddress = NULL;
	VIRTUALALLOC pVirtualAlloc     = NULL;
	NTFLUSHINSTRUCTIONCACHE pNtFlushInstructionCache = NULL;

	USHORT usCounter;

	// 此映像在内存中的初始位置
	ULONG_PTR uiLibraryAddress;
	// 内核的基地址，稍后是此映像新加载的基地址
	ULONG_PTR uiBaseAddress;

	// 用于处理内核导出表的变量
	ULONG_PTR uiAddressArray;
	ULONG_PTR uiNameArray;
	ULONG_PTR uiExportDir;
	ULONG_PTR uiNameOrdinals;
	DWORD dwHashValue;

	// 用于加载此映像的变量
	ULONG_PTR uiHeaderValue;
	ULONG_PTR uiValueA;
	ULONG_PTR uiValueB;
	ULONG_PTR uiValueC;
	ULONG_PTR uiValueD;
	ULONG_PTR uiValueE;

	// 步骤 0: 计算我们映像的当前基地址

	// 我们将从调用者的返回地址开始向后搜索。
	uiLibraryAddress = caller();

	// 向后循环搜索内存以找到我们映像的基地址
	// 我们不需要 SEH 风格的搜索，因为这不应该产生任何访问冲突
	while( TRUE )
	{
		if( ((PIMAGE_DOS_HEADER)uiLibraryAddress)->e_magic == IMAGE_DOS_SIGNATURE )
		{
			uiHeaderValue = ((PIMAGE_DOS_HEADER)uiLibraryAddress)->e_lfanew;
			// 一些 x64 dll 可能触发虚假签名 (IMAGE_DOS_SIGNATURE == 'POP r10')，
			// 我们对 e_lfanew 进行健全性检查，使用 1024 的上限阈值来避免问题。
			if( uiHeaderValue >= sizeof(IMAGE_DOS_HEADER) && uiHeaderValue < 1024 )
			{
				uiHeaderValue += uiLibraryAddress;
				// 如果我们找到了有效的 MZ/PE 头则跳出
				if( ((PIMAGE_NT_HEADERS)uiHeaderValue)->Signature == IMAGE_NT_SIGNATURE )
					break;
			}
		}
		uiLibraryAddress--;
	}

	// 步骤 1: 处理内核导出以获取我们加载器需要的函数...

	// 获取进程环境块
#ifdef WIN_X64
	uiBaseAddress = __readgsqword( 0x60 );
#else
#ifdef WIN_X86
	uiBaseAddress = __readfsdword( 0x30 );
#else WIN_ARM
	uiBaseAddress = *(DWORD *)( (BYTE *)_MoveFromCoprocessor( 15, 0, 13, 0, 2 ) + 0x30 );
#endif
#endif

	// 获取进程加载的模块。参考: http://msdn.microsoft.com/en-us/library/aa813708(VS.85).aspx
	uiBaseAddress = (ULONG_PTR)((_PPEB)uiBaseAddress)->pLdr;

	// 获取 InMemoryOrder 模块列表的第一个条目
	uiValueA = (ULONG_PTR)((PPEB_LDR_DATA)uiBaseAddress)->InMemoryOrderModuleList.Flink;
	while( uiValueA )
	{
		// 获取指向当前模块名称的指针（unicode 字符串）
		uiValueB = (ULONG_PTR)((PLDR_DATA_TABLE_ENTRY)uiValueA)->BaseDllName.pBuffer;
		// 将 bCounter 设置为循环的长度
		usCounter = ((PLDR_DATA_TABLE_ENTRY)uiValueA)->BaseDllName.Length;
		// 清除 uiValueC，它将存储模块名称的哈希值
		uiValueC = 0;

		// 计算模块名称的哈希值...
		do
		{
			uiValueC = ror( (DWORD)uiValueC );
			// 如果模块名称是小写则规范化为大写
			if( *((BYTE *)uiValueB) >= 'a' )
				uiValueC += *((BYTE *)uiValueB) - 0x20;
			else
				uiValueC += *((BYTE *)uiValueB);
			uiValueB++;
		} while( --usCounter );

		// 将哈希值与 kernel32.dll 的哈希值进行比较
		if( (DWORD)uiValueC == KERNEL32DLL_HASH )
		{
			// 获取此模块的基地址
			uiBaseAddress = (ULONG_PTR)((PLDR_DATA_TABLE_ENTRY)uiValueA)->DllBase;

			// 获取模块 NT 头的 VA
			uiExportDir = uiBaseAddress + ((PIMAGE_DOS_HEADER)uiBaseAddress)->e_lfanew;

			// uiNameArray = 模块导出目录条目的地址
			uiNameArray = (ULONG_PTR)&((PIMAGE_NT_HEADERS)uiExportDir)->OptionalHeader.DataDirectory[ IMAGE_DIRECTORY_ENTRY_EXPORT ];

			// 获取导出目录的 VA
			uiExportDir = ( uiBaseAddress + ((PIMAGE_DATA_DIRECTORY)uiNameArray)->VirtualAddress );

			// 获取名称指针数组的 VA
			uiNameArray = ( uiBaseAddress + ((PIMAGE_EXPORT_DIRECTORY )uiExportDir)->AddressOfNames );
			
			// 获取名称序号数组的 VA
			uiNameOrdinals = ( uiBaseAddress + ((PIMAGE_EXPORT_DIRECTORY )uiExportDir)->AddressOfNameOrdinals );

			usCounter = 3;

			// 循环直到我们仍有导入需要查找
			while( usCounter > 0 )
			{
				// 计算此函数名称的哈希值
				dwHashValue = hash( (char *)( uiBaseAddress + DEREF_32( uiNameArray ) )  );
				
				// 如果我们找到了想要的函数，就获取其虚拟地址
				if( dwHashValue == LOADLIBRARYA_HASH || dwHashValue == GETPROCADDRESS_HASH || dwHashValue == VIRTUALALLOC_HASH )
				{
					// 获取地址数组的 VA
					uiAddressArray = ( uiBaseAddress + ((PIMAGE_EXPORT_DIRECTORY )uiExportDir)->AddressOfFunctions );

					// 使用此函数的名称序号作为名称指针数组的索引
					uiAddressArray += ( DEREF_16( uiNameOrdinals ) * sizeof(DWORD) );

					// 存储此函数的 VA
					if( dwHashValue == LOADLIBRARYA_HASH )
						pLoadLibraryA = (LOADLIBRARYA)( uiBaseAddress + DEREF_32( uiAddressArray ) );
					else if( dwHashValue == GETPROCADDRESS_HASH )
						pGetProcAddress = (GETPROCADDRESS)( uiBaseAddress + DEREF_32( uiAddressArray ) );
					else if( dwHashValue == VIRTUALALLOC_HASH )
						pVirtualAlloc = (VIRTUALALLOC)( uiBaseAddress + DEREF_32( uiAddressArray ) );
			
					// 递减我们的计数器
					usCounter--;
				}

				// 获取下一个导出函数名称
				uiNameArray += sizeof(DWORD);

				// 获取下一个导出函数名称序号
				uiNameOrdinals += sizeof(WORD);
			}
		}
		else if( (DWORD)uiValueC == NTDLLDLL_HASH )
		{
			// 获取此模块的基地址
			uiBaseAddress = (ULONG_PTR)((PLDR_DATA_TABLE_ENTRY)uiValueA)->DllBase;

			// 获取模块 NT 头的 VA
			uiExportDir = uiBaseAddress + ((PIMAGE_DOS_HEADER)uiBaseAddress)->e_lfanew;

			// uiNameArray = 模块导出目录条目的地址
			uiNameArray = (ULONG_PTR)&((PIMAGE_NT_HEADERS)uiExportDir)->OptionalHeader.DataDirectory[ IMAGE_DIRECTORY_ENTRY_EXPORT ];

			// 获取导出目录的 VA
			uiExportDir = ( uiBaseAddress + ((PIMAGE_DATA_DIRECTORY)uiNameArray)->VirtualAddress );

			// 获取名称指针数组的 VA
			uiNameArray = ( uiBaseAddress + ((PIMAGE_EXPORT_DIRECTORY )uiExportDir)->AddressOfNames );
			
			// 获取名称序号数组的 VA
			uiNameOrdinals = ( uiBaseAddress + ((PIMAGE_EXPORT_DIRECTORY )uiExportDir)->AddressOfNameOrdinals );

			usCounter = 1;

			// 循环直到我们仍有导入需要查找
			while( usCounter > 0 )
			{
				// 计算此函数名称的哈希值
				dwHashValue = hash( (char *)( uiBaseAddress + DEREF_32( uiNameArray ) )  );
				
				// 如果我们找到了想要的函数，就获取其虚拟地址
				if( dwHashValue == NTFLUSHINSTRUCTIONCACHE_HASH )
				{
					// 获取地址数组的 VA
					uiAddressArray = ( uiBaseAddress + ((PIMAGE_EXPORT_DIRECTORY )uiExportDir)->AddressOfFunctions );

					// 使用此函数的名称序号作为名称指针数组的索引
					uiAddressArray += ( DEREF_16( uiNameOrdinals ) * sizeof(DWORD) );

					// 存储此函数的 VA
					if( dwHashValue == NTFLUSHINSTRUCTIONCACHE_HASH )
						pNtFlushInstructionCache = (NTFLUSHINSTRUCTIONCACHE)( uiBaseAddress + DEREF_32( uiAddressArray ) );

					// 递减我们的计数器
					usCounter--;
				}

				// 获取下一个导出函数名称
				uiNameArray += sizeof(DWORD);

				// 获取下一个导出函数名称序号
				uiNameOrdinals += sizeof(WORD);
			}
		}

		// 当我们找到所需的一切时停止搜索。
		if( pLoadLibraryA && pGetProcAddress && pVirtualAlloc && pNtFlushInstructionCache )
			break;

		// 获取下一个条目
		uiValueA = DEREF( uiValueA );
	}

	// 步骤 2: 将我们的映像加载到内存中的新永久位置...

	// 获取要加载的 PE 的 NT 头的 VA
	uiHeaderValue = uiLibraryAddress + ((PIMAGE_DOS_HEADER)uiLibraryAddress)->e_lfanew;

	// 为要加载的 DLL 分配所有内存。我们可以在任何地址加载，因为我们将
	// 重定位映像。同时将所有内存置零并标记为读、写和执行以避免任何问题。
	uiBaseAddress = (ULONG_PTR)pVirtualAlloc( NULL, ((PIMAGE_NT_HEADERS)uiHeaderValue)->OptionalHeader.SizeOfImage, MEM_RESERVE|MEM_COMMIT, PAGE_EXECUTE_READWRITE );

	// 我们现在必须复制头部
	uiValueA = ((PIMAGE_NT_HEADERS)uiHeaderValue)->OptionalHeader.SizeOfHeaders;
	uiValueB = uiLibraryAddress;
	uiValueC = uiBaseAddress;

	while( uiValueA-- )
		*(BYTE *)uiValueC++ = *(BYTE *)uiValueB++;

	// 步骤 3: 加载我们所有的节...

	// uiValueA = 第一个节的 VA
	uiValueA = ( (ULONG_PTR)&((PIMAGE_NT_HEADERS)uiHeaderValue)->OptionalHeader + ((PIMAGE_NT_HEADERS)uiHeaderValue)->FileHeader.SizeOfOptionalHeader );
	
	// 遍历所有节，将它们加载到内存中。
	uiValueE = ((PIMAGE_NT_HEADERS)uiHeaderValue)->FileHeader.NumberOfSections;
	while( uiValueE-- )
	{
		// uiValueB 是此节的 VA
		uiValueB = ( uiBaseAddress + ((PIMAGE_SECTION_HEADER)uiValueA)->VirtualAddress );

		// uiValueC 是此节数据的 VA
		uiValueC = ( uiLibraryAddress + ((PIMAGE_SECTION_HEADER)uiValueA)->PointerToRawData );

		// 复制节
		uiValueD = ((PIMAGE_SECTION_HEADER)uiValueA)->SizeOfRawData;

		while( uiValueD-- )
			*(BYTE *)uiValueB++ = *(BYTE *)uiValueC++;

		// 获取下一个节的 VA
		uiValueA += sizeof( IMAGE_SECTION_HEADER );
	}

	// 步骤 4: 处理我们映像的导入表...

	// uiValueB = 导入目录的地址
	uiValueB = (ULONG_PTR)&((PIMAGE_NT_HEADERS)uiHeaderValue)->OptionalHeader.DataDirectory[ IMAGE_DIRECTORY_ENTRY_IMPORT ];
	
	// 我们假设有一个导入表需要处理
	// uiValueC 是导入表中的第一个条目
	uiValueC = ( uiBaseAddress + ((PIMAGE_DATA_DIRECTORY)uiValueB)->VirtualAddress );
	
	// 遍历所有导入
	while( ((PIMAGE_IMPORT_DESCRIPTOR)uiValueC)->Name )
	{
		// 使用 LoadLibraryA 将导入的模块加载到内存中
		uiLibraryAddress = (ULONG_PTR)pLoadLibraryA( (LPCSTR)( uiBaseAddress + ((PIMAGE_IMPORT_DESCRIPTOR)uiValueC)->Name ) );

		// uiValueD = OriginalFirstThunk 的 VA
		uiValueD = ( uiBaseAddress + ((PIMAGE_IMPORT_DESCRIPTOR)uiValueC)->OriginalFirstThunk );
	
		// uiValueA = IAT 的 VA（通过 first thunk 而不是 origionalfirstthunk）
		uiValueA = ( uiBaseAddress + ((PIMAGE_IMPORT_DESCRIPTOR)uiValueC)->FirstThunk );

		// 遍历所有导入函数，如果没有名称则按序号导入
		while( DEREF(uiValueA) )
		{
			// 对 uiValueD 进行健全性检查，因为一些编译器只通过 FirstThunk 导入
			if( uiValueD && ((PIMAGE_THUNK_DATA)uiValueD)->u1.Ordinal & IMAGE_ORDINAL_FLAG )
			{
				// 获取模块 NT 头的 VA
				uiExportDir = uiLibraryAddress + ((PIMAGE_DOS_HEADER)uiLibraryAddress)->e_lfanew;

				// uiNameArray = 模块导出目录条目的地址
				uiNameArray = (ULONG_PTR)&((PIMAGE_NT_HEADERS)uiExportDir)->OptionalHeader.DataDirectory[ IMAGE_DIRECTORY_ENTRY_EXPORT ];

				// 获取导出目录的 VA
				uiExportDir = ( uiLibraryAddress + ((PIMAGE_DATA_DIRECTORY)uiNameArray)->VirtualAddress );

				// 获取地址数组的 VA
				uiAddressArray = ( uiLibraryAddress + ((PIMAGE_EXPORT_DIRECTORY )uiExportDir)->AddressOfFunctions );

				// 使用导入序号（- 导出序号基数）作为地址数组的索引
				uiAddressArray += ( ( IMAGE_ORDINAL( ((PIMAGE_THUNK_DATA)uiValueD)->u1.Ordinal ) - ((PIMAGE_EXPORT_DIRECTORY )uiExportDir)->Base ) * sizeof(DWORD) );

				// 为此导入函数修补地址
				DEREF(uiValueA) = ( uiLibraryAddress + DEREF_32(uiAddressArray) );
			}
			else
			{
				// 获取此函数按名称导入结构的 VA
				uiValueB = ( uiBaseAddress + DEREF(uiValueA) );

				// 使用 GetProcAddress 并为此导入函数修补地址
				DEREF(uiValueA) = (ULONG_PTR)pGetProcAddress( (HMODULE)uiLibraryAddress, (LPCSTR)((PIMAGE_IMPORT_BY_NAME)uiValueB)->Name );
			}
			// 获取下一个导入函数
			uiValueA += sizeof( ULONG_PTR );
			if( uiValueD )
				uiValueD += sizeof( ULONG_PTR );
		}

		// 获取下一个导入
		uiValueC += sizeof( IMAGE_IMPORT_DESCRIPTOR );
	}

	// 步骤 5: 处理我们映像的所有重定位...

	// 计算基地址差值并执行重定位（即使我们在期望的映像基地址加载）
	uiLibraryAddress = uiBaseAddress - ((PIMAGE_NT_HEADERS)uiHeaderValue)->OptionalHeader.ImageBase;

	// uiValueB = 重定位目录的地址
	uiValueB = (ULONG_PTR)&((PIMAGE_NT_HEADERS)uiHeaderValue)->OptionalHeader.DataDirectory[ IMAGE_DIRECTORY_ENTRY_BASERELOC ];

	// 检查是否存在任何重定位
	if( ((PIMAGE_DATA_DIRECTORY)uiValueB)->Size )
	{
		// uiValueC 现在是第一个条目（IMAGE_BASE_RELOCATION）
		uiValueC = ( uiBaseAddress + ((PIMAGE_DATA_DIRECTORY)uiValueB)->VirtualAddress );

		// 我们遍历所有条目...
		while( ((PIMAGE_BASE_RELOCATION)uiValueC)->SizeOfBlock )
		{
			// uiValueA = 此重定位块的 VA
			uiValueA = ( uiBaseAddress + ((PIMAGE_BASE_RELOCATION)uiValueC)->VirtualAddress );

			// uiValueB = 此重定位块中的条目数
			uiValueB = ( ((PIMAGE_BASE_RELOCATION)uiValueC)->SizeOfBlock - sizeof(IMAGE_BASE_RELOCATION) ) / sizeof( IMAGE_RELOC );

			// uiValueD 现在是当前重定位块中的第一个条目
			uiValueD = uiValueC + sizeof(IMAGE_BASE_RELOCATION);

			// 我们遍历当前块中的所有条目...
			while( uiValueB-- )
			{
				// 执行重定位，按要求跳过 IMAGE_REL_BASED_ABSOLUTE。
				// 我们不使用 switch 语句以避免编译器构建跳转表
				// 这将不是很好的位置无关！
				if( ((PIMAGE_RELOC)uiValueD)->type == IMAGE_REL_BASED_DIR64 )
					*(ULONG_PTR *)(uiValueA + ((PIMAGE_RELOC)uiValueD)->offset) += uiLibraryAddress;
				else if( ((PIMAGE_RELOC)uiValueD)->type == IMAGE_REL_BASED_HIGHLOW )
					*(DWORD *)(uiValueA + ((PIMAGE_RELOC)uiValueD)->offset) += (DWORD)uiLibraryAddress;
#ifdef WIN_ARM
				// 注意：在 ARM 上，编译器优化 /O2 似乎引入了一个偏移一个问题，可能是代码生成错误。使用 /O1 可以避免这个问题。
				else if( ((PIMAGE_RELOC)uiValueD)->type == IMAGE_REL_BASED_ARM_MOV32T )
				{	
					register DWORD dwInstruction;
					register DWORD dwAddress;
					register WORD wImm;
					// 获取 MOV.T 指令的 DWORD 值（我们在偏移量上加 4 以越过处理低字的第一个 MOV.W）
					dwInstruction = *(DWORD *)( uiValueA + ((PIMAGE_RELOC)uiValueD)->offset + sizeof(DWORD) );
					// 翻转字以获得期望的指令
					dwInstruction = MAKELONG( HIWORD(dwInstruction), LOWORD(dwInstruction) );
					// 健全性检查我们正在处理 MOV 指令...
					if( (dwInstruction & ARM_MOV_MASK) == ARM_MOVT )
					{
						// 提取编码的 16 位值（要重定位的地址的高部分）
						wImm  = (WORD)( dwInstruction & 0x000000FF);
						wImm |= (WORD)((dwInstruction & 0x00007000) >> 4);
						wImm |= (WORD)((dwInstruction & 0x04000000) >> 15);
						wImm |= (WORD)((dwInstruction & 0x000F0000) >> 4);
						// 将重定位应用到目标地址
						dwAddress = ( (WORD)HIWORD(uiLibraryAddress) + wImm ) & 0xFFFF;
						// 现在使用相同的操作码和寄存器参数创建一个新指令。
						dwInstruction  = (DWORD)( dwInstruction & ARM_MOV_MASK2 );
						// 修补重定位的地址...
						dwInstruction |= (DWORD)(dwAddress & 0x00FF);
						dwInstruction |= (DWORD)(dwAddress & 0x0700) << 4;
						dwInstruction |= (DWORD)(dwAddress & 0x0800) << 15;
						dwInstruction |= (DWORD)(dwAddress & 0xF000) << 4;
						// 现在翻转指令字并修补回代码...
						*(DWORD *)( uiValueA + ((PIMAGE_RELOC)uiValueD)->offset + sizeof(DWORD) ) = MAKELONG( HIWORD(dwInstruction), LOWORD(dwInstruction) );
					}
				}
#endif
				else if( ((PIMAGE_RELOC)uiValueD)->type == IMAGE_REL_BASED_HIGH )
					*(WORD *)(uiValueA + ((PIMAGE_RELOC)uiValueD)->offset) += HIWORD(uiLibraryAddress);
				else if( ((PIMAGE_RELOC)uiValueD)->type == IMAGE_REL_BASED_LOW )
					*(WORD *)(uiValueA + ((PIMAGE_RELOC)uiValueD)->offset) += LOWORD(uiLibraryAddress);

				// 获取当前重定位块中的下一个条目
				uiValueD += sizeof( IMAGE_RELOC );
			}

			// 获取重定位目录中的下一个条目
			uiValueC = uiValueC + ((PIMAGE_BASE_RELOCATION)uiValueC)->SizeOfBlock;
		}
	}

	// 步骤 6: 调用我们映像的入口点

	// uiValueA = 我们新加载的 DLL/EXE 入口点的 VA
	uiValueA = ( uiBaseAddress + ((PIMAGE_NT_HEADERS)uiHeaderValue)->OptionalHeader.AddressOfEntryPoint );

	// 我们必须刷新指令缓存以避免使用被我们的重定位处理更新的过时代码。
	pNtFlushInstructionCache( (HANDLE)-1, NULL, 0 );

	// 调用我们各自的入口点，伪造我们的 hInstance 值
#ifdef REFLECTIVEDLLINJECTION_VIA_LOADREMOTELIBRARYR
	// 如果我们通过 LoadRemoteLibraryR 注入 DLL，我们调用 DllMain 并传入我们的参数（通过 DllMain lpReserved 参数）
	((DLLMAIN)uiValueA)( (HINSTANCE)uiBaseAddress, DLL_PROCESS_ATTACH, lpParameter );
#else
	// 如果我们通过存根注入 DLL，我们调用不带参数的 DllMain
	((DLLMAIN)uiValueA)( (HINSTANCE)uiBaseAddress, DLL_PROCESS_ATTACH, NULL );
#endif

	// 步骤 8: 返回我们的新入口点地址，以便调用我们的任何东西都可以在需要时调用 DllMain()。
	return uiValueA;
}
//===============================================================================================//
#ifndef REFLECTIVEDLLINJECTION_CUSTOM_DLLMAIN

BOOL WINAPI DllMain( HINSTANCE hinstDLL, DWORD dwReason, LPVOID lpReserved )
{
    BOOL bReturnValue = TRUE;
	switch( dwReason ) 
    { 
		case DLL_QUERY_HMODULE:
			if( lpReserved != NULL )
				*(HMODULE *)lpReserved = hAppInstance;
			break;
		case DLL_PROCESS_ATTACH:
			hAppInstance = hinstDLL;
			break;
		case DLL_PROCESS_DETACH:
		case DLL_THREAD_ATTACH:
		case DLL_THREAD_DETACH:
            break;
    }
	return bReturnValue;
}

#endif
//===============================================================================================//
