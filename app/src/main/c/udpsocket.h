//
// Created by Kenji Miura on 2020/08/08.
//

#ifndef AUDIOTEST_UDPSOCKET_H
#define AUDIOTEST_UDPSOCKET_H

#include <stdio.h>

int openUDPSocket(const char* remoteHost, int remotePort, int localPort);
int sendUDPDatagram(int sock, const short* data, int len);
void closeUDPSocket(int sock);

#endif //AUDIOTEST_UDPSOCKET_H
