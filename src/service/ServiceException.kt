package service

class ServiceException(val status: Int, message: String) : RuntimeException(message)